package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PaymentStatusResponse(
    @JsonProperty("paymentCode")
    String paymentCode,
    
    @JsonProperty("status")
    String status,
    
    @JsonProperty("amount")
    Double amount,
    
    @JsonProperty("currency")
    String currency,
    
    @JsonProperty("expiresAt")
    LocalDateTime expiresAt,
    
    @JsonProperty("isExpired")
    Boolean isExpired,
    
    @JsonProperty("message")
    String message
) {}
