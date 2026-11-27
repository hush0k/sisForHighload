#!/bin/sh
set -e
if [ ! -s "$PGDATA/PG_VERSION" ]; then
  until pg_isready -h postgres-primary -U "$POSTGRES_USER" -d "$POSTGRES_DB"; do
    sleep 1
  done
  mkdir -p "$PGDATA"
  chown postgres:postgres "$PGDATA"
  chmod 700 "$PGDATA"
  PGPASSWORD="$REPLICATION_PASSWORD" gosu postgres pg_basebackup \
    -h postgres-primary -U replicator -D "$PGDATA" -R -X stream
fi
exec docker-entrypoint.sh "$@"
