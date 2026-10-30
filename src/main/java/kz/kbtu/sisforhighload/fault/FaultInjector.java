package kz.kbtu.sisforhighload.fault;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class FaultInjector {

    private final FaultProperties properties;

    public boolean shouldFail() {
        return properties.enabled() && randomChance(properties.failureRate());
    }

    public boolean shouldRespondSlowly() {
        return properties.enabled() && randomChance(properties.slowResponseRate());
    }

    public void delayResponse() {
        try {
            Thread.sleep(properties.slowResponseDelay());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean randomChance(double probability) {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }
}