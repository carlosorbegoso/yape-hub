package org.sky.dto.response.branch;

import java.util.List;

public record BranchAnalyticsResponse(
    List<BranchPerformanceData> branchPerformance,
    BranchComparison branchComparison
) {}
