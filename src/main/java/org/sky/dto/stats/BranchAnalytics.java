package org.sky.dto.stats;

import java.util.List;

public record BranchAnalytics(
    List<BranchPerformanceData> branchPerformance,
    BranchComparison branchComparison
) {}
