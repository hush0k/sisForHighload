package kz.kbtu.sisforhighload.counter.repository;

import kz.kbtu.sisforhighload.counter.entity.LikeId;
import kz.kbtu.sisforhighload.counter.entity.ViewDedupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface ViewDedupRepository extends JpaRepository<ViewDedupEntity, LikeId> {

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO view_dedup (post_id, user_id, counted_at)
            VALUES (:postId, :userId, :now)
            ON CONFLICT (post_id, user_id) DO UPDATE SET counted_at = :now
            WHERE view_dedup.counted_at < :threshold
            """)
    int upsertIfExpired(long postId, long userId, LocalDateTime now, LocalDateTime threshold);
}
