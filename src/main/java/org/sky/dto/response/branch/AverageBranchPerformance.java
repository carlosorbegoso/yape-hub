package org.sky.dto.response.branch;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AverageBranchPerformance(
    Double averageSales,
    Double averageTransactions,
    Double averagePerformanceScore
) {
    public static AverageBranchPerformance empty() {
        return new AverageBranchPerformance(0.0, 0.0, 0.0);
    }
}
