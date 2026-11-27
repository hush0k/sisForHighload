package kz.kbtu.sisforhighload.counter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tuning")
public record TuningProperties(
        int stage,
        int poolSize,
        String replicaUrl
) {

    private static final int LAST_STAGE = 5;

    public TuningProperties {
        if (stage < 0 || stage > LAST_STAGE) {
            throw new IllegalArgumentException("tuning.stage должен быть от 0 до 5");
        }
        if (poolSize < 1) {
            throw new IllegalArgumentException("tuning.pool-size должен быть больше 0");
        }
        if (stage == LAST_STAGE && (replicaUrl == null || replicaUrl.isBlank())) {
            throw new IllegalArgumentException("на стадии 5 нужен tuning.replica-url");
        }
    }

    public boolean hasPool() {
        return stage >= 1;
    }

    public boolean hasPrimaryKey() {
        return stage >= 2;
    }

    public boolean countersFromTable() {
        return stage >= 3;
    }

    public boolean hasPartitions() {
        return stage >= 4;
    }

    public boolean hasReplica() {
        return stage >= 5;
    }

    public int flywayTarget() {
        if (hasPartitions()) {
            return 3;
        }
        return hasPrimaryKey() ? 2 : 1;
    }
}
