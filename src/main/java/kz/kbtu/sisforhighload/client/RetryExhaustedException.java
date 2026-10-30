package kz.kbtu.sisforhighload.client;

public class RetryExhaustedException extends RuntimeException {

    public RetryExhaustedException(String operation, int attempts, Throwable lastError) {
        super(operation + " failed after " + attempts + " attempts", lastError);
    }
}