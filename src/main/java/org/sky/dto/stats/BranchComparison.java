package org.sky.dto.stats;

public record BranchComparison(
    BranchSummary topPerformingBranch,
    BranchSummary lowestPerformingBranch,
    AverageBranchPerformance averagePerformance
) {}