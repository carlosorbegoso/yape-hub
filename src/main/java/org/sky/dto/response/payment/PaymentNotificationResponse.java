package org.sky.dto.response.payment;

import java.time.LocalDateTime;

public record PaymentNotificationResponse(
    Long paymentId,
    Double amount,
    String senderName,
    String yapeCode,
    String status,
    LocalDateTime timestamp,
    String message
) {}
