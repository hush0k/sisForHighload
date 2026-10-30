package kz.kbtu.sisforhighload.counter.exception;

public class ViewTooShortException extends RuntimeException {

    public ViewTooShortException(String message) {
        super(message);
    }
}