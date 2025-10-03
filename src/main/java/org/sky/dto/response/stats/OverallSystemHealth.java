package org.sky.dto.response.stats;

public record OverallSystemHealth(
    Double totalSystemSales,
    Long totalSystemTransactions,
    Double systemUptime,
    Double averageResponseTime,
    Double errorRate,
    Long activeUsers
) {
    public static OverallSystemHealth empty() {
        return new OverallSystemHealth(0.0, 0L, 0.0, 0.0, 0.0, 0L);
    }
}