package org.sky.util;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.sky.dto.response.ErrorResponse;
import org.sky.exception.ValidationException;

import java.util.Map;

/**
 * Utility class for handling controller errors - SIMPLIFIED VERSION
 */
public class ControllerErrorHandler {
    
    private static final Logger log = Logger.getLogger(ControllerErrorHandler.class);

    /**
     * Handles controller errors and creates appropriate HTTP responses
     */
    public static Response handleControllerError(Throwable throwable) {
        log.error("🚨 Controller error: " + throwable.getMessage());
        
        // Manejar ValidationException
        if (throwable instanceof ValidationException validationException) {
            return Response.status(validationException.getStatus())
                .entity(ErrorResponse.create(validationException.getMessage(), validationException.getErrorCode()))
                .build();
        }
        
        // Manejar SecurityException como 401
        if (throwable instanceof SecurityException) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(ErrorResponse.create("Authentication failed: " + throwable.getMessage(), "UNAUTHORIZED"))
                .build();
        }
        
        // Manejar IllegalArgumentException como 400
        if (throwable instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse.create("Invalid request: " + throwable.getMessage(), "BAD_REQUEST"))
                .build();
        }
        
        // Para otros errores, retornar error interno del servidor
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(ErrorResponse.create("Internal server error", "INTERNAL_ERROR"))
            .build();
    }
    
    /**
     * Crea una respuesta de error 401 Unauthorized
     */
    public static Response createUnauthorizedResponse(String message) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(ErrorResponse.create(message != null ? message : "Authentication required", "UNAUTHORIZED"))
            .build();
    }
}
