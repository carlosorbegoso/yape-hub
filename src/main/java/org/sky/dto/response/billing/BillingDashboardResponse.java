package org.sky.dto.response.billing;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;
import java.util.List;

@RegisterForReflection

public record BillingDashboardResponse(
        Long adminId,
        Object tokenStatus,
        SubscriptionStatusResponse subscriptionStatus,
        List<PaymentHistoryResponse> recentPayments,
        BillingSummaryResponse billingSummary,
        LocalDateTime lastUpdated
) {}