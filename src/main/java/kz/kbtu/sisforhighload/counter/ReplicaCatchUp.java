package kz.kbtu.sisforhighload.counter;

import jakarta.annotation.PostConstruct;
import kz.kbtu.sisforhighload.counter.repository.ReplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReplicaCatchUp {

    private static final int MAX_ATTEMPTS = 600;

    private static final long PAUSE_MILLIS = 500;

    private final TuningProperties tuning;

    private final ReplicationRepository replicationRepository;

    private final ObjectProvider<BenchSeeder> benchSeeder;

    @PostConstruct
    void waitForReplica() throws InterruptedException {
        benchSeeder.ifAvailable(BenchSeeder::seedIfEmpty);
        if (!tuning.hasReplica()) {
            return;
        }
        String target = replicationRepository.currentWalPosition();
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            if (replicationRepository.replicaReached(target)) {
                return;
            }
            Thread.sleep(PAUSE_MILLIS);
        }
        throw new IllegalStateException("Реплика не догнала primary за 5 минут");
    }
}
