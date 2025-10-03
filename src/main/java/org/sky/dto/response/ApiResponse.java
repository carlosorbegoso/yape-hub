package org.sky.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    String error
) {
    // Constructor compacto - validaciones y normalizaciones
    public ApiResponse {
        // Validaciones
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        
        // Normalizaciones
        message = message.trim();
        error = error != null ? error.trim() : null;
        
        // Validaciones de consistencia
        if (success && error != null) {
            throw new IllegalArgumentException("Success response cannot have error message");
        }
        if (!success && error == null) {
            error = message; // Para respuestas de error, usar message como error
        }
    }
    
    // Constructores de conveniencia
    public ApiResponse(boolean success, String message) {
        this(success, message, null, null);
    }
    
    public ApiResponse(boolean success, String message, T data) {
        this(success, message, data, null);
    }
    
    // Métodos estáticos de conveniencia
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, message);
    }
    
    public static <T> ApiResponse<T> error(String message, String error) {
        return new ApiResponse<>(false, message, null, error);
    }
    
    // Métodos de conveniencia para acceso a campos
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isError() {
        return !success;
    }
}
