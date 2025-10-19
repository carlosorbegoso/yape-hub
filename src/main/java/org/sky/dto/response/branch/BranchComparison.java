package org.sky.dto.response.branch;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record BranchComparison(
    BranchSummary topPerformingBranch,
    BranchSummary lowestPerformingBranch,
    AverageBranchPerformance averagePerformance
) {
    public static BranchComparison empty() {
        return new BranchComparison(
            BranchSummary.empty(),
            BranchSummary.empty(),
            AverageBranchPerformance.empty()
        );
    }
}
