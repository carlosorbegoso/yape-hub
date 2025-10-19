package org.sky.dto.response.stats;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record RevenueBreakdown(
    Double totalRevenue,
    List<RevenueByBranch> revenueByBranch,
    RevenueGrowth revenueGrowth
) {
    public static RevenueBreakdown empty() {
        return new RevenueBreakdown(0.0, List.of(), RevenueGrowth.empty());
    }
}
