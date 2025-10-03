package org.sky.dto.response.stats;

public record SystemMetrics(
    OverallSystemHealth overallSystemHealth,
    PaymentSystemMetrics paymentSystemMetrics,
    UserEngagement userEngagement
) {
    public static SystemMetrics empty() {
        return new SystemMetrics(
            OverallSystemHealth.empty(),
            PaymentSystemMetrics.empty(),
            UserEngagement.empty()
        );
    }
}