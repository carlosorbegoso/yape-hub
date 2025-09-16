package org.sky.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record PaymentRejectRequest(
    @NotNull(message = "sellerId es requerido")
    Long sellerId,
    
    @NotNull(message = "paymentId es requerido")
    Long paymentId,
    
    String reason
) {}
