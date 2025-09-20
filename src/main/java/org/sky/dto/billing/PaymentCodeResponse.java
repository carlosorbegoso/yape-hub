package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PaymentCodeResponse(
    @JsonProperty("paymentCode")
    String paymentCode,
    
    @JsonProperty("yapeNumber")
    String yapeNumber,
    
    @JsonProperty("amount")
    Double amount,
    
    @JsonProperty("currency")
    String currency,
    
    @JsonProperty("expiresAt")
    LocalDateTime expiresAt,
    
    @JsonProperty("instructions")
    String instructions
) {}
