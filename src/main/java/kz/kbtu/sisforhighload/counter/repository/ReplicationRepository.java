package kz.kbtu.sisforhighload.counter.repository;

import kz.kbtu.sisforhighload.counter.entity.PostCounterEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

public interface ReplicationRepository extends Repository<PostCounterEntity, Long> {

    @Query(nativeQuery = true, value = "SELECT CAST(pg_current_wal_lsn() AS text)")
    String currentWalPosition();

    @Transactional(readOnly = true)
    @Query(nativeQuery = true, value = """
            SELECT pg_wal_lsn_diff(CAST(:position AS pg_lsn), pg_last_wal_replay_lsn()) <= 0
            """)
    boolean replicaReached(String position);
}
