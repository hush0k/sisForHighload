EXPLAIN (ANALYZE, BUFFERS)
SELECT post_id AS "postId", likes_count AS "likesCount", views_count AS "viewsCount"
FROM post_counters
WHERE post_id = ANY(ARRAY[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]::bigint[]);
