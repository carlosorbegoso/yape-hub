package org.sky.util;

import jakarta.ws.rs.core.Response;
import org.sky.dto.response.ErrorResponse;
import org.sky.exception.ValidationException;

import java.util.Map;

/**
 * Utility class for handling controller errors
 * Centralizes error response creation logic
 */
public class ControllerErrorHandler {

    /**
     * Handles controller errors and creates appropriate HTTP responses
     */
    public static Response handleControllerError(Throwable throwable) {
        if (throwable instanceof ValidationException validationException) {
            ErrorResponse errorResponse = new ErrorResponse(
                validationException.getMessage(),
                validationException.getErrorCode(),
                validationException.getDetails(),
                java.time.Instant.now()
            );
            return Response.status(validationException.getStatus()).entity(errorResponse).build();
        }
        
        // For other errors, return a generic error response
        ErrorResponse errorResponse = new ErrorResponse(
            "Error interno del servidor: " + throwable.getMessage(),
            "INTERNAL_ERROR",
            Map.of("timestamp", java.time.Instant.now()),
            java.time.Instant.now()
        );
        return Response.status(500).entity(errorResponse).build();
    }
}
