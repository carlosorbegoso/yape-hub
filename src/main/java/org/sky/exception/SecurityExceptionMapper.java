package org.sky.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.sky.dto.response.ErrorResponse;

import java.util.Map;

/**
 * Exception Mapper para manejar SecurityException (errores de autorización)
 * Convierte excepciones de seguridad en respuestas JSON estructuradas
 */
@Provider
public class SecurityExceptionMapper implements ExceptionMapper<SecurityException> {
    
    private static final Logger log = Logger.getLogger(SecurityExceptionMapper.class);
    
    @Override
    public Response toResponse(SecurityException exception) {
        log.warn("🚫 Security violation: " + exception.getMessage());
        
        // Determinar si es 401 (no autenticado) o 403 (no autorizado)
        String message = exception.getMessage();
        boolean isUnauthorized = message != null && (
            message.toLowerCase().contains("unauthorized") ||
            message.toLowerCase().contains("invalid token") ||
            message.toLowerCase().contains("token expired") ||
            message.toLowerCase().contains("authentication")
        );
        
        if (isUnauthorized) {
            // Error 401 - No autenticado
            ErrorResponse errorResponse = ErrorResponse.withDetails(
                "Authentication failed. " + message,
                "UNAUTHORIZED",
                Map.of(
                    "error_type", "authentication_failed",
                    "hint", "Check your JWT token validity and format"
                )
            );
            
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorResponse)
                    .build();
        } else {
            // Error 403 - No autorizado (autenticado pero sin permisos)
            ErrorResponse errorResponse = ErrorResponse.withDetails(
                "Access forbidden. " + message,
                "FORBIDDEN",
                Map.of(
                    "error_type", "insufficient_permissions",
                    "hint", "You don't have permission to access this resource"
                )
            );
            
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(errorResponse)
                    .build();
        }
    }
}