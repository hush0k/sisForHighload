BEGIN;
EXPLAIN (ANALYZE, BUFFERS)
INSERT INTO view_dedup (post_id, user_id, counted_at)
VALUES (500, 5000001, now() AT TIME ZONE 'UTC')
ON CONFLICT (post_id, user_id) DO UPDATE SET counted_at = now() AT TIME ZONE 'UTC'
WHERE view_dedup.counted_at < (now() AT TIME ZONE 'UTC') - interval '1 hour';
ROLLBACK;
