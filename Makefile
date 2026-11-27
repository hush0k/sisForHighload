.PHONY: help init docker-check up down reset app psql-primary psql-replica logs stage bench-all explain k6 results wait-app

-include .env

COMPOSE ?= docker-compose
POSTGRES_DB ?= sisforhighload
POSTGRES_USER ?= sis
PRIMARY_PORT ?= 5442
REPLICA_PORT ?= 5443
APP_PORT ?= 8081
N ?= 5
S ?= smoke

help:
	@echo "make init                 создать .env из .env.example"
	@echo "make up                   поднять primary и replica"
	@echo "make down                 остановить всё"
	@echo "make reset                остановить всё и удалить тома"
	@echo "make app                  запустить приложение (полностью настроенное, stage 5)"
	@echo "make psql-primary         psql в primary"
	@echo "make psql-replica         psql в replica"
	@echo "make logs                 логи всех сервисов"
	@echo "make stage N=0..5         приложение на стадии N, explain и k6"
	@echo "make bench-all            reset, up и стадии 0..5"
	@echo "make explain N=0..5       EXPLAIN ANALYZE для стадии N"
	@echo "make k6 S=smoke|mixed|hot_post|lag N=0..5"
	@echo "make results              показать итоговые таблицы k6"

init:
	@test -f .env || cp .env.example .env
	@echo ".env готов"

docker-check:
	@docker info >/dev/null 2>&1 || { echo "запусти: colima start"; exit 1; }

up: docker-check init
	$(COMPOSE) up -d --wait postgres-primary postgres-replica

down: docker-check
	$(COMPOSE) --profile app --profile bench down

reset: docker-check
	$(COMPOSE) --profile app --profile bench down -v

app: docker-check up
	$(COMPOSE) --profile app up --build app

psql-primary: docker-check
	$(COMPOSE) exec postgres-primary psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)

psql-replica: docker-check
	$(COMPOSE) exec postgres-replica psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)

logs: docker-check
	$(COMPOSE) --profile app logs -f

wait-app:
	@i=0; until curl -sf -o /dev/null -H "X-User-Id: 1" "http://localhost:$(APP_PORT)/api/v1/counters?post_ids=1"; do i=$$((i+1)); test $$i -le 900 || { echo "приложение не поднялось"; exit 1; }; sleep 2; done

stage: docker-check up
	TUNING_STAGE=$(N) APP_PROFILE=bench $(COMPOSE) --profile app up -d --build --force-recreate app
	$(MAKE) wait-app
	$(MAKE) explain N=$(N)
	$(MAKE) k6 S=smoke N=$(N)
	$(MAKE) k6 S=mixed N=$(N)
	$(MAKE) k6 S=hot_post N=$(N)
	@if [ "$(N)" = "5" ]; then $(MAKE) k6 S=lag N=$(N); fi

bench-all:
	$(MAKE) reset
	$(MAKE) up
	$(MAKE) stage N=0
	$(MAKE) stage N=1
	$(MAKE) stage N=2
	$(MAKE) stage N=3
	$(MAKE) stage N=4
	$(MAKE) stage N=5

explain: docker-check
	@mkdir -p benchmark-results/explain
	@if [ "$(N)" -ge 2 ]; then LIKE=like_insert_on_conflict; else LIKE=like_insert_not_exists; fi; \
	{ cat docker/explain/likes_single_post.sql docker/explain/likes_count.sql docker/explain/counters_batch.sql docker/explain/liked_by_me.sql docker/explain/$$LIKE.sql docker/explain/view_dedup.sql; } \
	| $(COMPOSE) exec -T postgres-primary psql -U $(POSTGRES_USER) -d $(POSTGRES_DB) > benchmark-results/explain/stage$(N).txt
	@if [ "$(N)" = "5" ]; then cat docker/explain/counters_batch.sql docker/explain/liked_by_me.sql | $(COMPOSE) exec -T postgres-replica psql -U $(POSTGRES_USER) -d $(POSTGRES_DB) > benchmark-results/explain/stage5-replica.txt; fi
	@echo "benchmark-results/explain/stage$(N).txt"

k6: docker-check
	@mkdir -p benchmark-results
	$(COMPOSE) --profile bench run --rm -e STAGE=$(N) k6 run /scripts/$(S).js

results:
	@cat benchmark-results/*.md
