package org.sky.dto.response.notification;

import java.time.LocalDateTime;

public record YapeNotificationResponse(
    Long notificationId,
    String transactionId,
    Double amount,
    String senderPhone,
    String senderName,
    String receiverPhone,
    String status,
    LocalDateTime processedAt,
    String message
) {}
