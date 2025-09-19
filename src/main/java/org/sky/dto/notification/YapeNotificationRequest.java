package org.sky.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record YapeNotificationRequest(
    @NotNull(message = "AdminId es requerido")
    @JsonProperty("adminId")
    Long adminId,
    
    @NotBlank(message = "Notificación encriptada es requerida")
    @JsonProperty("encryptedNotification")
    String encryptedNotification,
    
    @NotBlank(message = "Device fingerprint es requerido")
    @JsonProperty("deviceFingerprint")
    String deviceFingerprint,
    
    @NotNull(message = "Timestamp es requerido")
    @JsonProperty("timestamp")
    Long timestamp,
    
    @NotBlank(message = "Deduplication hash es requerido")
    @JsonProperty("deduplicationHash")
    String deduplicationHash
) {
}
