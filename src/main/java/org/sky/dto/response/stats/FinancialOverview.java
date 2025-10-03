package org.sky.dto.response.stats;

public record FinancialOverview(
    RevenueBreakdown revenueBreakdown,
   CostAnalysis costAnalysis
) {}
