package kz.kbtu.sisforhighload.client;

import lombok.Getter;

@Getter
public class HttpStatusException extends RuntimeException {

    private final int statusCode;

    public HttpStatusException(int statusCode, String body) {
        super("HTTP " + statusCode + ": " + body);
        this.statusCode = statusCode;
    }

    public boolean isServerUnavailable() {
        return statusCode == 502 || statusCode == 503 || statusCode == 504;
    }
}