package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentUploadResponse(
    @JsonProperty("paymentId")
    Long paymentId,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("status")
    String status
) {}
