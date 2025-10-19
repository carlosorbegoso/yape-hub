package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
