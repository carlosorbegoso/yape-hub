package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record PaymentHistoryResponse(
        @JsonProperty("paymentId") Long paymentId,
        @JsonProperty("adminId") Long adminId,
        @JsonProperty("paymentType") String paymentType,
        @JsonProperty("amount") Double amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("status") String status,
        @JsonProperty("paymentMethod") String paymentMethod,
        @JsonProperty("description") String description,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("processedAt") LocalDateTime processedAt,
        @JsonProperty("referenceId") String referenceId
) {}
