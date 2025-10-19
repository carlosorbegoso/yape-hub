package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record FinancialOverview(
    RevenueBreakdown revenueBreakdown,
    CostAnalysis costAnalysis
) {
    public static FinancialOverview empty() {
        return new FinancialOverview(RevenueBreakdown.empty(), CostAnalysis.empty());
    }
}
