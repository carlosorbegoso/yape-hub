package org.sky.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

public record PaymentNotificationRequest(
    @NotNull(message = "AdminId es requerido")
    @JsonProperty("adminId")
    Long adminId,
    
    @NotNull(message = "Monto es requerido")
    @Positive(message = "Monto debe ser positivo")
    @JsonProperty("amount")
    Double amount,
    
    @NotBlank(message = "Nombre del remitente es requerido")
    @JsonProperty("senderName")
    String senderName,
    
    @NotBlank(message = "Código de Yape es requerido")
    @JsonProperty("yapeCode")
    String yapeCode,
    
    @NotBlank(message = "Deduplication hash es requerido")
    @JsonProperty("deduplicationHash")
    String deduplicationHash
) {}
