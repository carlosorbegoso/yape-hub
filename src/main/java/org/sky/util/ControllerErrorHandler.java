package org.sky.util;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.sky.dto.response.ErrorResponse;
import org.sky.exception.ValidationException;

import java.util.Map;

/**
 * Utility class for handling controller errors
 * Centralizes error response creation logic
 */
public class ControllerErrorHandler {
    
    private static final Logger log = Logger.getLogger(ControllerErrorHandler.class);

    /**
     * Handles controller errors and creates appropriate HTTP responses
     */
    public static Response handleControllerError(Throwable throwable) {
        log.error("🚨 Controller error: " + throwable.getMessage(), throwable);
        
        // Manejar ValidationException
        if (throwable instanceof ValidationException validationException) {
            ErrorResponse errorResponse = ErrorResponse.withDetails(
                validationException.getMessage(),
                validationException.getErrorCode(),
                validationException.getDetails()
            );
            return Response.status(validationException.getStatus()).entity(errorResponse).build();
        }
        
        // Manejar SecurityException (401/403)
        if (throwable instanceof SecurityException securityException) {
            String message = securityException.getMessage();
            boolean isAuthenticationError = message != null && (
                message.toLowerCase().contains("authorization header") ||
                message.toLowerCase().contains("bearer") ||
                message.toLowerCase().contains("token") ||
                message.toLowerCase().contains("invalid access token") ||
                message.toLowerCase().contains("expired") ||
                message.toLowerCase().contains("malformed") ||
                message.toLowerCase().contains("authentication") ||
                message.toLowerCase().contains("unable to parse")
            );
            
            if (isAuthenticationError) {
                ErrorResponse errorResponse = ErrorResponse.withDetails(
                    "Authentication failed: " + message,
                    "UNAUTHORIZED",
                    Map.of(
                        "error_type", "authentication_failed",
                        "hint", "Please provide a valid JWT token"
                    )
                );
                return Response.status(Response.Status.UNAUTHORIZED).entity(errorResponse).build();
            } else {
                ErrorResponse errorResponse = ErrorResponse.withDetails(
                    "Access forbidden: " + message,
                    "FORBIDDEN",
                    Map.of(
                        "error_type", "insufficient_permissions",
                        "hint", "You don't have permission to access this resource"
                    )
                );
                return Response.status(Response.Status.FORBIDDEN).entity(errorResponse).build();
            }
        }
        
        // Manejar IllegalArgumentException (400)
        if (throwable instanceof IllegalArgumentException illegalArgException) {
            ErrorResponse errorResponse = ErrorResponse.withDetails(
                "Invalid request: " + illegalArgException.getMessage(),
                "BAD_REQUEST",
                Map.of(
                    "error_type", "invalid_argument",
                    "hint", "Check your request parameters"
                )
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
        }
        
        // Manejar RuntimeException genérica
        if (throwable instanceof RuntimeException runtimeException) {
            String message = runtimeException.getMessage();
            
            // Detectar errores de negocio comunes
            if (message != null) {
                if (message.toLowerCase().contains("not found")) {
                    ErrorResponse errorResponse = ErrorResponse.withDetails(
                        message,
                        "NOT_FOUND",
                        Map.of("error_type", "resource_not_found")
                    );
                    return Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
                }
                
                if (message.toLowerCase().contains("already exists")) {
                    ErrorResponse errorResponse = ErrorResponse.withDetails(
                        message,
                        "CONFLICT",
                        Map.of("error_type", "resource_conflict")
                    );
                    return Response.status(Response.Status.CONFLICT).entity(errorResponse).build();
                }
            }
        }
        
        // Para otros errores, retornar error interno del servidor
        ErrorResponse errorResponse = ErrorResponse.withDetails(
            "Internal server error occurred",
            "INTERNAL_ERROR",
            Map.of(
                "error_type", "internal_server_error",
                "hint", "Please try again later or contact support",
                "original_message", throwable.getMessage() != null ? throwable.getMessage() : "Unknown error"
            )
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
    }
    
    /**
     * Crea una respuesta de error 401 Unauthorized
     */
    public static Response createUnauthorizedResponse(String message) {
        ErrorResponse errorResponse = ErrorResponse.withDetails(
            message != null ? message : "Authentication required",
            "UNAUTHORIZED",
            Map.of(
                "error_type", "authentication_required",
                "hint", "Include a valid JWT token in the Authorization header"
            )
        );
        return Response.status(Response.Status.UNAUTHORIZED).entity(errorResponse).build();
    }
    
    /**
     * Crea una respuesta de error 403 Forbidden
     */
    public static Response createForbiddenResponse(String message) {
        ErrorResponse errorResponse = ErrorResponse.withDetails(
            message != null ? message : "Access forbidden",
            "FORBIDDEN",
            Map.of(
                "error_type", "insufficient_permissions",
                "hint", "You don't have permission to access this resource"
            )
        );
        return Response.status(Response.Status.FORBIDDEN).entity(errorResponse).build();
    }
}
