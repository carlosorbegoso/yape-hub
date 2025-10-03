package org.sky.dto.response.billing;

import java.time.LocalDateTime;

public record PaymentStatusResponse(
    String paymentCode,
    String status,
    Double amount,
    String currency,
    LocalDateTime expiresAt,
    Boolean isExpired,
    String message
) {}
