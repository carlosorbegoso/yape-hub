package org.sky.exception;

import jakarta.ws.rs.core.Response;

import java.util.Map;

public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;

    protected DomainException(String message, String errorCode) {
        this(message, errorCode, Map.of());
    }

    protected DomainException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? details : Map.of();
    }

    protected DomainException(String message, String errorCode, Map<String, Object> details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details != null ? details : Map.of();
    }

    public abstract Response.Status getStatus();

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}