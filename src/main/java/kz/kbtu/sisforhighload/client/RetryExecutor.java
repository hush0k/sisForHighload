package kz.kbtu.sisforhighload.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
public class RetryExecutor {

    private final int maxAttempts;
    private final Duration initialBackoff;
    private final AtomicLong retryCount = new AtomicLong();

    public <T> T execute(String operation, Callable<T> call, Predicate<Exception> isRetryable) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.call();
            } catch (Exception error) {
                if (!isRetryable.test(error)) {
                    throw toRuntimeException(error);
                }
                lastError = error;
                if (attempt < maxAttempts) {
                    retryCount.incrementAndGet();
                    log.warn("{} attempt {}/{} failed: {}", operation, attempt, maxAttempts, error.getMessage());
                    sleep(backoffFor(attempt));
                }
            }
        }
        throw new RetryExhaustedException(operation, maxAttempts, lastError);
    }

    public long getRetryCount() {
        return retryCount.get();
    }
    private Duration backoffFor(int attempt) {
        long baseMillis = initialBackoff.toMillis() * (1L << (attempt - 1));
        long jitterMillis = ThreadLocalRandom.current().nextLong(baseMillis / 2 + 1);
        return Duration.ofMillis(baseMillis + jitterMillis);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", exception);
        }
    }

    private RuntimeException toRuntimeException(Exception error) {
        if (error instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(error);
    }
}