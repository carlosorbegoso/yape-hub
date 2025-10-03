package org.sky.dto.response.billing;

import java.time.LocalDateTime;

public record BillingSummaryResponse(
        Double totalSpent,
        String currency,
        LocalDateTime nextBillingDate,
        Boolean autoRenewal,
        String paymentMethod
) {}
