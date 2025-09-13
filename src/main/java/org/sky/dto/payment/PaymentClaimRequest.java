package org.sky.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record PaymentClaimRequest(
    @NotNull(message = "SellerId es requerido")
    @JsonProperty("sellerId")
    Long sellerId,
    
    @NotNull(message = "PaymentId es requerido")
    @JsonProperty("paymentId")
    Long paymentId
) {}
