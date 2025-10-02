package org.sky.dto.stats;

public record OverallSystemHealth(
    Double totalSystemSales,
    Long totalSystemTransactions,
    Double systemUptime,
    Double averageResponseTime,
    Double errorRate,
    Long activeUsers
) {}