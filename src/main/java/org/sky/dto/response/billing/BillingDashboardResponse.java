package org.sky.dto.response.billing;

import java.time.LocalDateTime;
import java.util.List;

public record BillingDashboardResponse(
        Long adminId,
        Object tokenStatus,
        SubscriptionStatusResponse subscriptionStatus,
        List<PaymentHistoryResponse> recentPayments,
        MonthlyUsageResponse monthlyUsage,
        BillingSummaryResponse billingSummary,
        LocalDateTime lastUpdated
) {}