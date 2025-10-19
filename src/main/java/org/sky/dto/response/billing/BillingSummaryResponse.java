package org.sky.dto.response.billing;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record BillingSummaryResponse(
        Double totalSpent,
        String currency,
        LocalDateTime nextBillingDate,
        Boolean autoRenewal,
        String paymentMethod
) {}
