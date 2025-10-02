package org.sky.dto.stats;

public record AverageBranchPerformance(
    Double averageSales,
    Double averageTransactions,
    Double averagePerformanceScore
) {}