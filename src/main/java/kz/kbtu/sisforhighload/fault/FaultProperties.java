package kz.kbtu.sisforhighload.fault;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "fault")
public record FaultProperties(
        boolean enabled,
        double failureRate,
        double slowResponseRate,
        Duration slowResponseDelay
) {
}