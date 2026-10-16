package com.pacgram.common.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
        String message,
        String errorCode,
        int status,
        String path,
        List<FieldError> fieldErrors,
        Instant timestamp) {

    public record FieldError(String field, String message) {
    }
}
