package kz.kbtu.sisforhighload.counter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "counter")
public record CounterProperties(
        Duration minViewDuration,
        Duration viewDedupTtl
) {
}