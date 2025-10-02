package org.sky.dto.stats.financial;

import java.util.List;

public record FinancialOverviewResponse(
    RevenueBreakdown revenueBreakdown,
    CostAnalysis costAnalysis,
    List<RevenueByBranch> revenueByBranch
) {}