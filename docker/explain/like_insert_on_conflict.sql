BEGIN;
EXPLAIN (ANALYZE, BUFFERS)
INSERT INTO likes (post_id, user_id, created_at)
VALUES (500, 5000001, now() AT TIME ZONE 'UTC')
ON CONFLICT (post_id, user_id) DO NOTHING;
ROLLBACK;
