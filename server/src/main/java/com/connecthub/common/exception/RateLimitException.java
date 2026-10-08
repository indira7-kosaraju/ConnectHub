package com.connecthub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RateLimitException extends ApiException {

    private final long retryAfterSeconds;

    public RateLimitException(String message, long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
