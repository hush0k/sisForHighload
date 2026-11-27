package kz.kbtu.sisforhighload.counter.repository;

import kz.kbtu.sisforhighload.counter.entity.LikeEntity;
import kz.kbtu.sisforhighload.counter.entity.LikeId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

public interface BenchSeedRepository extends Repository<LikeEntity, LikeId> {

    @Transactional(readOnly = true)
    @Query(nativeQuery = true, value = """
            SELECT NOT EXISTS (SELECT 1 FROM likes) AND NOT EXISTS (SELECT 1 FROM post_counters)
            """)
    boolean tablesAreEmpty();

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = """
            WITH inserted AS (
                INSERT INTO likes (post_id, user_id, created_at)
                SELECT p.post_id,
                       ((p.post_id * 7919 + k.n * (:users / :likesPerPost)) % :users) + 1,
                       now() AT TIME ZONE 'UTC'
                FROM generate_series(1, :posts) AS p(post_id)
                CROSS JOIN generate_series(0, :likesPerPost - 1) AS k(n)
                RETURNING post_id
            )
            INSERT INTO post_counters (post_id, likes_count, views_count, updated_at)
            SELECT post_id, count(*), 0, now() AT TIME ZONE 'UTC'
            FROM inserted
            GROUP BY post_id
            """)
    int seedLikesAndCounters(long posts, int likesPerPost, long users);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "ANALYZE")
    void analyze();
}
