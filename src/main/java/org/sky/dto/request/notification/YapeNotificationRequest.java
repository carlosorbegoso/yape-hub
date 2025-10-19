package org.sky.dto.request.notification;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record YapeNotificationRequest(
    @NotNull(message = "AdminId es requerido")
    Long adminId,
    
    @NotBlank(message = "Notificación encriptada es requerida")
    String encryptedNotification,
    
    @NotBlank(message = "Device fingerprint es requerido")
    String deviceFingerprint,
    
    @NotNull(message = "Timestamp es requerido")
    Long timestamp,
    
    @NotBlank(message = "Deduplication hash es requerido")
    String deduplicationHash
) {
}
