package org.sky.dto.stats.system;

public record SystemMetricsResponse(
    OverallSystemHealth overallSystemHealth,
    PaymentSystemMetrics paymentSystemMetrics,
    UserEngagement userEngagement
) {
    
    public record OverallSystemHealth(
        Double totalSystemSales,
        Long totalSystemTransactions,
        Double systemUptime,
        Double averageResponseTime,
        Double errorRate,
        Long activeUsers
    ) {}
    
    public record PaymentSystemMetrics(
        Long totalPaymentsProcessed,
        Long pendingPayments,
        Long confirmedPayments,
        Long rejectedPayments,
        Double averageConfirmationTime,
        Double paymentSuccessRate
    ) {}
    
    public record UserEngagement(
        Long dailyActiveUsers,
        Long weeklyActiveUsers,
        Long monthlyActiveUsers,
        Double averageSessionDuration,
        FeatureUsage featureUsage
    ) {}
    
    public record FeatureUsage(
        Double qrScanner,
        Double paymentManagement,
        Double analytics,
        Double notifications
    ) {}
}
