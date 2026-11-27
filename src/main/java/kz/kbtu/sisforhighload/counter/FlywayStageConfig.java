package kz.kbtu.sisforhighload.counter;

import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FlywayStageConfig {

    private final TuningProperties tuning;

    @Bean
    public FlywayConfigurationCustomizer flywayTargetCustomizer() {
        return configuration -> configuration.target(String.valueOf(tuning.flywayTarget()));
    }

    @Bean
    public FlywayMigrationStrategy forwardOnlyStrategy() {
        return flyway -> {
            ensureNotAhead(flyway);
            flyway.migrate();
        };
    }

    private void ensureNotAhead(Flyway flyway) {
        MigrationInfo current = flyway.info().current();
        if (current != null && Integer.parseInt(current.getVersion().getVersion()) > tuning.flywayTarget()) {
            throw new IllegalStateException("База уже на версии " + current.getVersion()
                    + ", стадия " + tuning.stage() + " требует " + tuning.flywayTarget()
                    + ": сначала сделай make reset");
        }
    }
}
