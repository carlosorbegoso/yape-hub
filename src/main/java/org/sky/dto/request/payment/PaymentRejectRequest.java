package org.sky.dto.request.payment;

import jakarta.validation.constraints.NotNull;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentRejectRequest(
    @NotNull(message = "sellerId es requerido")
    Long sellerId,
    
    @NotNull(message = "paymentId es requerido")
    Long paymentId,
    
    String reason
) {}
