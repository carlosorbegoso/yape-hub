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
) {
    // Constructor compacto - validaciones y normalizaciones
    public PaymentNotificationRequest {
        // Validaciones
        if (adminId == null) {
            throw new IllegalArgumentException("AdminId es requerido");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Monto debe ser positivo");
        }
        if (senderName == null || senderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre del remitente es requerido");
        }
        if (yapeCode == null || yapeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Código de Yape es requerido");
        }
        if (deduplicationHash == null || deduplicationHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Deduplication hash es requerido");
        }
        
        // Normalizaciones
        senderName = senderName.trim();
        yapeCode = yapeCode.trim();
        deduplicationHash = deduplicationHash.trim();
    }
    
    // Constructor de conveniencia
    public static PaymentNotificationRequest create(Long adminId, Double amount, String senderName, String yapeCode, String deduplicationHash) {
        return new PaymentNotificationRequest(adminId, amount, senderName, yapeCode, deduplicationHash);
    }
}
