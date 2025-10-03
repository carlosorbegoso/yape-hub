package org.sky.service.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.sky.dto.response.ErrorResponse;

import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class SecurityErrorHandler {

    public Response createSecurityErrorResponse(String message, int statusCode) {
        return createSecurityErrorResponse(message, "SECURITY_ERROR", statusCode);
    }

    public Response createSecurityErrorResponse(String message, String errorCode, int statusCode) {
        ErrorResponse errorResponse = new ErrorResponse(
            message,
            errorCode,
            Map.of("timestamp", Instant.now()),
            Instant.now()
        );
        return Response.status(statusCode).entity(errorResponse).build();
    }

    public Response handleSecurityException(Throwable throwable) {
        if (throwable instanceof SecurityException) {
            return createSecurityErrorResponse(throwable.getMessage(), 401);
        }
        
        if (throwable instanceof IllegalArgumentException) {
            return createSecurityErrorResponse(throwable.getMessage(), "VALIDATION_ERROR", 400);
        }
        
        if (throwable instanceof org.sky.exception.ValidationException validationException) {
            return createSecurityErrorResponse(
                validationException.getMessage(), 
                validationException.getErrorCode(), 
                validationException.getStatus().getStatusCode()
            );
        }
        
        if (throwable.getMessage() != null && 
            throwable.getMessage().contains("duplicate key value violates unique constraint")) {
            return createSecurityErrorResponse(
                "Duplicate transaction code. Notification saved but transaction already exists in the system", 
                "DUPLICATE_ERROR",
                400);
        }
        
        return createSecurityErrorResponse("Security error: " + throwable.getMessage(), "INTERNAL_ERROR", 500);
    }
}
