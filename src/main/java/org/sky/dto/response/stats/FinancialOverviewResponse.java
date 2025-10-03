package org.sky.dto.response.stats;

import java.util.List;

public record FinancialOverviewResponse(
    RevenueBreakdown revenueBreakdown,
    CostAnalysis costAnalysis,
    List<RevenueByBranch> revenueByBranch
) {}