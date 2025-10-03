package org.sky.dto.request.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

public record PaymentNotificationRequest(
    @NotNull(message = "AdminId es requerido")
    Long adminId,
    
    @NotNull(message = "Monto es requerido")
    @Positive(message = "Monto debe ser positivo")
    Double amount,
    
    @NotBlank(message = "Nombre del remitente es requerido")
    String senderName,
    
    @NotBlank(message = "Código de Yape es requerido")
    String yapeCode,
    
    @NotBlank(message = "Deduplication hash es requerido")
    String deduplicationHash
) {}
