package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record BillingDashboardResponse(
        @JsonProperty("adminId") Long adminId,
        @JsonProperty("tokenStatus") Object tokenStatus,
        @JsonProperty("subscriptionStatus") SubscriptionStatusResponse subscriptionStatus,
        @JsonProperty("recentPayments") List<PaymentHistoryResponse> recentPayments,
        @JsonProperty("monthlyUsage") MonthlyUsageResponse monthlyUsage,
        @JsonProperty("billingSummary") BillingSummaryResponse billingSummary,
        @JsonProperty("lastUpdated") LocalDateTime lastUpdated
) {}