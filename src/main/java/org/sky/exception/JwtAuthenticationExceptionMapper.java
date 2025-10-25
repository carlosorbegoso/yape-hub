package org.sky.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.sky.dto.response.ErrorResponse;

import java.util.Map;

/**
 * Exception Mapper para manejar errores relacionados con JWT
 * Captura RuntimeException que contengan mensajes relacionados con JWT
 */
@Provider
public class JwtAuthenticationExceptionMapper implements ExceptionMapper<RuntimeException> {
    
    private static final Logger log = Logger.getLogger(JwtAuthenticationExceptionMapper.class);
    
    @Override
    public Response toResponse(RuntimeException exception) {
        String message = exception.getMessage();
        
        // Solo manejar errores relacionados con JWT
        if (message != null && isJwtRelatedError(message)) {
            log.warn("🔑 JWT authentication error: " + message);
            
            String errorType = determineJwtErrorType(message);
            String hint = getHintForJwtError(errorType);
            
            ErrorResponse errorResponse = ErrorResponse.withDetails(
                "JWT authentication failed: " + message,
                "UNAUTHORIZED",
                Map.of(
                    "error_type", errorType,
                    "hint", hint,
                    "token_status", "invalid"
                )
            );
            
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorResponse)
                    .build();
        }
        
        // Si no es un error JWT, no manejar aquí (dejar que otros mappers lo manejen)
        throw exception;
    }
    
    /**
     * Determina si el error está relacionado con JWT
     */
    private boolean isJwtRelatedError(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("jwt") ||
               lowerMessage.contains("token") ||
               lowerMessage.contains("expired") ||
               lowerMessage.contains("invalid signature") ||
               lowerMessage.contains("malformed") ||
               lowerMessage.contains("authentication") ||
               lowerMessage.contains("unauthorized");
    }
    
    /**
     * Determina el tipo específico de error JWT
     */
    private String determineJwtErrorType(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("expired")) {
            return "jwt_expired";
        } else if (lowerMessage.contains("invalid") || lowerMessage.contains("malformed")) {
            return "jwt_invalid";
        } else if (lowerMessage.contains("signature")) {
            return "jwt_signature_invalid";
        } else if (lowerMessage.contains("not found")) {
            return "jwt_missing";
        } else {
            return "jwt_error";
        }
    }
    
    /**
     * Proporciona hints específicos según el tipo de error
     */
    private String getHintForJwtError(String errorType) {
        return switch (errorType) {
            case "jwt_expired" -> "Your JWT token has expired. Please login again to get a new token";
            case "jwt_invalid" -> "Your JWT token is invalid. Please login again";
            case "jwt_signature_invalid" -> "Your JWT token signature is invalid";
            case "jwt_missing" -> "JWT token is missing. Please include it in the Authorization header";
            default -> "Check your JWT token format and validity";
        };
    }
}