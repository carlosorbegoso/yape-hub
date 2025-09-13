package org.sky.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String message,
    String code,
    Map<String, Object> details,
    Instant timestamp
) {
}