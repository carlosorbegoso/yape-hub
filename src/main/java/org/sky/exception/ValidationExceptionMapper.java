package org.sky.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.sky.dto.response.ErrorResponse;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    
    private static final Logger log = Logger.getLogger(ValidationExceptionMapper.class);
    
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        log.warnf("Validation Exception caught: %s", exception.getMessage());
        
        Map<String, Object> validationErrors = new HashMap<>();
        
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            String field = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            Object invalidValue = violation.getInvalidValue();
            
            Map<String, Object> fieldError = new HashMap<>();
            fieldError.put("message", message);
            fieldError.put("invalidValue", invalidValue != null ? invalidValue : "null");
            validationErrors.put(field, fieldError);
        }
        
        return Response.status(422)
                .entity(new ErrorResponse(
                        "Validation failed",
                        "VALIDATION_ERROR",
                        Map.of("validationErrors", validationErrors),
                        java.time.Instant.now()
                ))
                .build();
    }
}
