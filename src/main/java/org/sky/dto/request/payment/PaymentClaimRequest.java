package org.sky.dto.request.payment;

import jakarta.validation.constraints.NotNull;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentClaimRequest(
    @NotNull(message = "SellerId es requerido")
    Long sellerId,
    
    @NotNull(message = "PaymentId es requerido")
    Long paymentId
) {}
