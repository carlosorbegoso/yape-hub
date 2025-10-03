package org.sky.dto.response.branch;

import java.util.List;

public record BranchAnalytics(
    List<BranchPerformanceData> branchPerformance,
    BranchComparison branchComparison
) {}
