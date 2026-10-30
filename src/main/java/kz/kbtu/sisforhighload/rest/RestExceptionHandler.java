package kz.kbtu.sisforhighload.rest;

import kz.kbtu.sisforhighload.counter.exception.InvalidViewTokenException;
import kz.kbtu.sisforhighload.counter.exception.ViewTooShortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler({InvalidViewTokenException.class, ViewTooShortException.class})
    public ProblemDetail handleBadView(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}