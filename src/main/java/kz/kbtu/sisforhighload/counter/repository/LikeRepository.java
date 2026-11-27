package kz.kbtu.sisforhighload.counter.repository;

import kz.kbtu.sisforhighload.counter.entity.LikeEntity;
import kz.kbtu.sisforhighload.counter.entity.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface LikeRepository extends JpaRepository<LikeEntity, LikeId> {

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO likes (post_id, user_id, created_at)
            SELECT :postId, :userId, :now
            WHERE NOT EXISTS (SELECT 1 FROM likes WHERE post_id = :postId AND user_id = :userId)
            """)
    int insertIfAbsent(long postId, long userId, LocalDateTime now);

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO likes (post_id, user_id, created_at)
            VALUES (:postId, :userId, :now)
            ON CONFLICT (post_id, user_id) DO NOTHING
            """)
    int insertOnConflictDoNothing(long postId, long userId, LocalDateTime now);

    @Modifying
    @Query(nativeQuery = true, value = """
            DELETE FROM likes WHERE post_id = :postId AND user_id = :userId
            """)
    int deleteLike(long postId, long userId);

    @Query(nativeQuery = true, value = """
            SELECT post_id AS "postId", count(*) AS "likesCount"
            FROM likes WHERE post_id = ANY(:postIds) GROUP BY post_id
            """)
    List<LikeCountRow> countLikesByPostIds(Long[] postIds);

    @Query(nativeQuery = true, value = """
            SELECT post_id FROM likes WHERE user_id = :userId AND post_id = ANY(:postIds)
            """)
    List<Long> findLikedPostIds(long userId, Long[] postIds);
}
