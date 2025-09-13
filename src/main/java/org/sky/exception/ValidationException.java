package org.sky.exception;

import jakarta.ws.rs.core.Response;

import java.util.Map;

public class ValidationException extends DomainException {
    
    protected ValidationException(String message, String errorCode, Map<String, Object> details) {
        super(message, errorCode, details);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.BAD_REQUEST;
    }

    // Factory methods for common validation errors
    public static ValidationException duplicateField(String field, String value) {
        return new ValidationException(
            String.format("Field '%s' with value '%s' already exists", field, value),
            "DUPLICATE_FIELD",
            Map.of("field", field, "value", value)
        );
    }

    public static ValidationException invalidField(String field, String value, String reason) {
        return new ValidationException(
            String.format("Invalid %s: %s - %s", field, value, reason),
            "INVALID_FIELD",
            Map.of("field", field, "value", value, "reason", reason)
        );
    }

    public static ValidationException requiredField(String field) {
        return new ValidationException(
            String.format("Field '%s' is required", field),
            "REQUIRED_FIELD",
            Map.of("field", field)
        );
    }

    public static ValidationException invalidFormat(String field, String expectedFormat) {
        return new ValidationException(
            String.format("Field '%s' has invalid format. Expected: %s", field, expectedFormat),
            "INVALID_FORMAT",
            Map.of("field", field, "expectedFormat", expectedFormat)
        );
    }
}