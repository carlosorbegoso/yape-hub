package org.sky.dto.response.stats;

public record OverviewMetrics(
    Double totalSales,
    Long totalTransactions,
    Double averageTransactionValue,
    Double salesGrowth,
    Double transactionGrowth,
    Double averageGrowth
) {}