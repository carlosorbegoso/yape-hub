package org.sky.dto.response.stats;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record FinancialOverviewResponse(
    RevenueBreakdown revenueBreakdown,
    CostAnalysis costAnalysis,
    List<RevenueByBranch> revenueByBranch
) {}
