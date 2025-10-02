package org.sky.dto.stats.system;

import org.sky.dto.stats.OverallSystemHealth;
import org.sky.dto.stats.PaymentSystemMetrics;
import org.sky.dto.stats.UserEngagement;

public record SystemMetricsResponse(
    OverallSystemHealth overallSystemHealth,
    PaymentSystemMetrics paymentSystemMetrics,
    UserEngagement userEngagement
) {
}
