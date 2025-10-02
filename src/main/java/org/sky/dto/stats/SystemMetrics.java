package org.sky.dto.stats;

public record SystemMetrics(
    OverallSystemHealth overallSystemHealth,
    PaymentSystemMetrics paymentSystemMetrics,
    UserEngagement userEngagement
) {}