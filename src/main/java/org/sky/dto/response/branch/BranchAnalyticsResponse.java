package org.sky.dto.response.branch;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record BranchAnalyticsResponse(
    List<BranchPerformanceData> branchPerformance,
    BranchComparison branchComparison
) {}
