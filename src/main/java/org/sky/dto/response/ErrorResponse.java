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
    // Constructor compacto - validaciones y normalizaciones
    public ErrorResponse {
        // Validaciones
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Error message cannot be null or empty");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Error code cannot be null or empty");
        }
        
        // Normalizaciones
        message = message.trim();
        code = code.trim().toUpperCase();
        
        // Valores por defecto
        if (timestamp == null) timestamp = Instant.now();
        if (details == null) details = Map.of();
    }
    
    // Constructor de conveniencia para errores simples
    public static ErrorResponse create(String message, String code) {
        return new ErrorResponse(message, code, Map.of(), Instant.now());
    }
    
    // Constructor con detalles
    public static ErrorResponse withDetails(String message, String code, Map<String, Object> details) {
        return new ErrorResponse(message, code, details, Instant.now());
    }
    
    // Constructor para errores de validación
    public static ErrorResponse validationError(String message, Map<String, Object> validationErrors) {
        return new ErrorResponse(message, "VALIDATION_ERROR", validationErrors, Instant.now());
    }
    
    // Constructor para errores de negocio
    public static ErrorResponse businessError(String message) {
        return new ErrorResponse(message, "BUSINESS_ERROR", Map.of(), Instant.now());
    }
    
    // Constructor para errores internos
    public static ErrorResponse internalError(String message) {
        return new ErrorResponse(message, "INTERNAL_ERROR", Map.of(), Instant.now());
    }
}