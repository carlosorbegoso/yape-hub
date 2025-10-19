package org.sky.dto.response.notification;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record YapeAuditResponse(
    Long auditId,
    Long adminId,
    String encryptedNotification,
    String deviceFingerprint,
    Long timestamp,
    String deduplicationHash,
    String decryptionStatus,
    String decryptionError,
    Double extractedAmount,
    String extractedSenderName,
    String extractedYapeCode,
    String transactionId,
    Long paymentNotificationId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
