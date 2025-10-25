package org.sky.exception;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.sky.dto.response.ErrorResponse;

import java.util.Map;

/**
 * Exception Mapper para manejar errores 401 Unauthorized
 * Convierte excepciones de autenticación en respuestas JSON estructuradas
 */
@Provider
public class UnauthorizedExceptionMapper implements ExceptionMapper<NotAuthorizedException> {
    
    private static final Logger log = Logger.getLogger(UnauthorizedExceptionMapper.class);
    
    @Override
    public Response toResponse(NotAuthorizedException exception) {
        log.warn("🔒 Unauthorized access attempt: " + exception.getMessage());
        
        // Crear respuesta de error estructurada
        ErrorResponse errorResponse = ErrorResponse.withDetails(
            "Access denied. Please provide valid authentication credentials.",
            "UNAUTHORIZED",
            Map.of(
                "error_type", "authentication_required",
                "hint", "Include a valid JWT token in the Authorization header",
                "format", "Authorization: Bearer <your-jwt-token>"
            )
        );
        
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(errorResponse)
                .build();
    }
}