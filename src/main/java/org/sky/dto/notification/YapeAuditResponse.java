package org.sky.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record YapeAuditResponse(
    @JsonProperty("auditId")
    Long auditId,
    
    @JsonProperty("adminId")
    Long adminId,
    
    @JsonProperty("encryptedNotification")
    String encryptedNotification,
    
    @JsonProperty("deviceFingerprint")
    String deviceFingerprint,
    
    @JsonProperty("timestamp")
    Long timestamp,
    
    @JsonProperty("deduplicationHash")
    String deduplicationHash,
    
    @JsonProperty("decryptionStatus")
    String decryptionStatus,
    
    @JsonProperty("decryptionError")
    String decryptionError,
    
    @JsonProperty("extractedAmount")
    Double extractedAmount,
    
    @JsonProperty("extractedSenderName")
    String extractedSenderName,
    
    @JsonProperty("extractedYapeCode")
    String extractedYapeCode,
    
    @JsonProperty("transactionId")
    String transactionId,
    
    @JsonProperty("paymentNotificationId")
    Long paymentNotificationId,
    
    @JsonProperty("createdAt")
    LocalDateTime createdAt,
    
    @JsonProperty("updatedAt")
    LocalDateTime updatedAt
) {}
