package org.sky.dto.response.billing;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentHistoryResponse(
        Long paymentId,
        Long adminId,
        String paymentType,
        Double amount,
        String currency,
        String status,
        String paymentMethod,
        String description,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        String referenceId
) {}
