package org.sky.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public record ErrorResponse(
    String message,
    String code,
    Map<String, Object> details,
    Instant timestamp
) {
    public ErrorResponse {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Error code cannot be null or empty");
        }

        message = message.trim();
        code = code.trim().toUpperCase();

        if (timestamp == null) timestamp = Instant.now();
        if (details == null) details = Map.of();
    }

    public static ErrorResponse create(String message, String code) {
        return new ErrorResponse(message, code, Map.of(), Instant.now());
    }

    public static ErrorResponse validationError(String message, Map<String, Object> validationErrors) {
        return new ErrorResponse(message, "VALIDATION_ERROR", validationErrors, Instant.now());
    }

}