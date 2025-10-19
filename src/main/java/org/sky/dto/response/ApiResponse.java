package org.sky.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    String error
) {
    public ApiResponse {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        message = message.trim();
        error = error != null ? error.trim() : null;

        if (success && error != null) {
            throw new IllegalArgumentException("Success response cannot have error message");
        }
        if (!success && error == null) {
            error = message;
        }
    }

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
    
    public static <T> ApiResponse<T> error(String message, String error) {
        return new ApiResponse<>(false, message, null, error);
    }

    public boolean isSuccess() {
        return success;
    }
    
    public boolean isError() {
        return !success;
    }
}
