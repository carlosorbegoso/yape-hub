package org.sky.exception;

import jakarta.ws.rs.core.Response;

import java.util.Map;

public abstract class ResourceNotFoundException extends DomainException {
    
    protected ResourceNotFoundException(String message, String errorCode, Map<String, Object> details) {
        super(message, errorCode, details);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.NOT_FOUND;
    }

    public static ResourceNotFoundException byField(String resourceType, String field, Object value) {
        return new ResourceNotFoundException(
            String.format("%s with %s '%s' not found", resourceType, field, value),
            "RESOURCE_NOT_FOUND",
            Map.of("resourceType", resourceType, "field", field, "value", value)
        ) {};
    }
}