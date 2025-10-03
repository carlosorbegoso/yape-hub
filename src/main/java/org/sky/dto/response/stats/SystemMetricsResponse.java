package org.sky.dto.response.stats;


public record SystemMetricsResponse(
    OverallSystemHealth overallSystemHealth,
    PaymentSystemMetrics paymentSystemMetrics,
    UserEngagement userEngagement
) {}
