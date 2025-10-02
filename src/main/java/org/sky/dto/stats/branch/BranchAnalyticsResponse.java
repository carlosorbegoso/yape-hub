package org.sky.dto.stats.branch;

import org.sky.dto.stats.BranchComparison;
import org.sky.dto.stats.BranchPerformanceData;

import java.util.List;

public record BranchAnalyticsResponse(
    List<BranchPerformanceData> branchPerformance,
    BranchComparison branchComparison
) {



}
