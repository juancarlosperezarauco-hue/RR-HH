package com.hrcoresystem.hrcore_api.common.response;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        String message,
        List<String> errors,
        Instant timestamp
) {
    public static ApiErrorResponse of(String message, List<String> errors) {
        return new ApiErrorResponse(false, message, errors, Instant.now());
    }
}