package org.sky.dto.request.payment;

import jakarta.validation.constraints.NotNull;

public record PaymentClaimRequest(
    @NotNull(message = "SellerId es requerido")
    Long sellerId,
    
    @NotNull(message = "PaymentId es requerido")
    Long paymentId
) {}
