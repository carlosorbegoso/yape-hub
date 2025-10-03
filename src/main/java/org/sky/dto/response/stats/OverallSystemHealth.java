package org.sky.dto.response.stats;

public record OverallSystemHealth(
    Double totalSystemSales,
    Long totalSystemTransactions,
    Double systemUptime,
    Double averageResponseTime,
    Double errorRate,
    Long activeUsers
) {}