package com.connecthub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base type for domain errors that map directly to an HTTP status.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public String code() {
        return status.name();
    }
}
