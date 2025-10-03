package org.sky.dto.response.branch;

public record BranchComparison(
    BranchSummary topPerformingBranch,
    BranchSummary lowestPerformingBranch,
    AverageBranchPerformance averagePerformance
) {}