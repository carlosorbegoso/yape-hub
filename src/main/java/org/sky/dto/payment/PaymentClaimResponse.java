package org.sky.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PaymentClaimResponse(
    @JsonProperty("paymentId")
    Long paymentId,
    
    @JsonProperty("amount")
    Double amount,
    
    @JsonProperty("senderName")
    String senderName,
    
    @JsonProperty("yapeCode")
    String yapeCode,
    
    @JsonProperty("status")
    String status,
    
    @JsonProperty("timestamp")
    LocalDateTime timestamp,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("confirmedBy")
    Long confirmedBy,
    
    @JsonProperty("confirmedAt")
    LocalDateTime confirmedAt
) {}
