package org.sky.dto.response.branch;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record BranchAnalytics(
    List<BranchPerformanceData> branchPerformance,
    BranchComparison branchComparison
) {
    public static BranchAnalytics empty() {
        return new BranchAnalytics(List.of(), BranchComparison.empty());
    }
}
