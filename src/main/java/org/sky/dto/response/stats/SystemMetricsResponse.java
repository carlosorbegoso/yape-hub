package org.sky.dto.response.stats;


import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SystemMetricsResponse(
    OverallSystemHealth overallSystemHealth,
    PaymentSystemMetrics paymentSystemMetrics,
    UserEngagement userEngagement
) {}
