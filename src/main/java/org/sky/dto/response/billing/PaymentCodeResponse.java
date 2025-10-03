package org.sky.dto.response.billing;

import java.time.LocalDateTime;

public record PaymentCodeResponse(
    String paymentCode,
    String yapeNumber,
    Double amount,
    String currency,
    LocalDateTime expiresAt,
    String instructions
) {}
