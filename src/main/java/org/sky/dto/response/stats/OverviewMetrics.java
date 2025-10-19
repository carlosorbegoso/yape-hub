package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record OverviewMetrics(
    Double totalSales,
    Long totalTransactions,
    Double averageTransactionValue,
    Double salesGrowth,
    Double transactionGrowth,
    Double averageGrowth
) {
    public static OverviewMetrics empty() {
        return new OverviewMetrics(0.0, 0L, 0.0, 0.0, 0.0, 0.0);
    }
}
