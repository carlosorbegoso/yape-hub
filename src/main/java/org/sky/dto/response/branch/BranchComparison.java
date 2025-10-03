package org.sky.dto.response.branch;

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