package org.sky.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    String error
) {
    public ApiResponse(boolean success, String message) {
        this(success, message, null, null);
    }
    
    public ApiResponse(boolean success, String message, T data) {
        this(success, message, data, null);
    }
    
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message);
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, message);
    }
    
    // Métodos de conveniencia para acceso a campos
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isError() {
        return !success;
    }
}
