package kz.kbtu.sisforhighload.counter.repository;

import kz.kbtu.sisforhighload.counter.entity.PostCounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PostCounterRepository extends JpaRepository<PostCounterEntity, Long> {

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO post_counters (post_id, likes_count, views_count, updated_at)
            VALUES (:postId, 1, 0, :now)
            ON CONFLICT (post_id) DO UPDATE
            SET likes_count = post_counters.likes_count + 1, updated_at = :now
            """)
    int incrementLikes(long postId, LocalDateTime now);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE post_counters SET likes_count = likes_count - 1, updated_at = :now
            WHERE post_id = :postId
            """)
    int decrementLikes(long postId, LocalDateTime now);

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO post_counters (post_id, likes_count, views_count, updated_at)
            VALUES (:postId, 0, 1, :now)
            ON CONFLICT (post_id) DO UPDATE
            SET views_count = post_counters.views_count + 1, updated_at = :now
            """)
    int incrementViews(long postId, LocalDateTime now);

    @Query(nativeQuery = true, value = """
            SELECT post_id AS "postId", likes_count AS "likesCount", views_count AS "viewsCount"
            FROM post_counters WHERE post_id = ANY(:postIds)
            """)
    List<PostCounterRow> findCountersByPostIds(Long[] postIds);
}
