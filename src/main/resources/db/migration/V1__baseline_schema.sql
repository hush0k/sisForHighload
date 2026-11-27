CREATE TABLE likes (
    post_id BIGINT,
    user_id BIGINT,
    created_at TIMESTAMP
);

CREATE TABLE post_counters (
    post_id BIGINT PRIMARY KEY,
    likes_count BIGINT,
    views_count BIGINT,
    updated_at TIMESTAMP
);

CREATE TABLE view_dedup (
    post_id BIGINT,
    user_id BIGINT,
    counted_at TIMESTAMP,
    PRIMARY KEY (post_id, user_id)
);
