package org.sky.dto.response.billing;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentStatusResponse(
    String paymentCode,
    String status,
    Double amount,
    String currency,
    LocalDateTime expiresAt,
    Boolean isExpired,
    String message
) {}
