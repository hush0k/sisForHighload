EXPLAIN (ANALYZE, BUFFERS)
SELECT post_id AS "postId", count(*) AS "likesCount"
FROM likes
WHERE post_id = ANY(ARRAY[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]::bigint[])
GROUP BY post_id;
