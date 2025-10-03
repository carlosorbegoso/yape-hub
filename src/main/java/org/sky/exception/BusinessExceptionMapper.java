package org.sky.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.sky.dto.response.ErrorResponse;


import java.time.Instant;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<DomainException> {
    private static final Logger log = Logger.getLogger(BusinessExceptionMapper.class);

    @Override
    public Response toResponse(DomainException exception) {
        log.warnf("Business Exception caught: %s - %s [code: %s]",
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception.getErrorCode());

        if (!exception.getDetails().isEmpty()) {
            log.debugf("Business Exception details: %s", exception.getDetails());
        }

        return Response.status(exception.getStatus())
                .entity(new ErrorResponse(
                        exception.getMessage(),
                        exception.getErrorCode(),
                        exception.getDetails(),
                        Instant.now()
                ))
                .build();
    }
}