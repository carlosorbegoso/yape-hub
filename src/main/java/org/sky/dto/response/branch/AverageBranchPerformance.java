package org.sky.dto.response.branch;

public record AverageBranchPerformance(
    Double averageSales,
    Double averageTransactions,
    Double averagePerformanceScore
) {}