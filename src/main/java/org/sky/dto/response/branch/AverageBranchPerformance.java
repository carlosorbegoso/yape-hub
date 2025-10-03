package org.sky.dto.response.branch;

public record AverageBranchPerformance(
    Double averageSales,
    Double averageTransactions,
    Double averagePerformanceScore
) {
    public static AverageBranchPerformance empty() {
        return new AverageBranchPerformance(0.0, 0.0, 0.0);
    }
}