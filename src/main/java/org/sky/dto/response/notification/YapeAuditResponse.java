package org.sky.dto.response.notification;

import java.time.LocalDateTime;

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
