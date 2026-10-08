package com.connecthub.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform envelope for every REST response: { success, data, error, timestamp }.
 * The client types this envelope by hand ({@code lib/api/types.ts}); only {@code data}'s type comes from OpenAPI.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, Instant.now());
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message, null), Instant.now());
    }

    public static ApiResponse<Void> fail(String code, String message, Map<String, String> fields) {
        return new ApiResponse<>(false, null, new ApiError(code, message, fields), Instant.now());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiError(String code, String message, Map<String, String> fields) {
    }
}
