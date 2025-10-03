package org.sky.dto.response.billing;

import java.time.LocalDateTime;

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
