package org.sky.dto.stats.financial;

import java.util.List;

public record FinancialOverviewResponse(
    RevenueBreakdown revenueBreakdown,
    CostAnalysis costAnalysis
) {
    
    public record RevenueBreakdown(
        Double totalRevenue,
        List<RevenueByBranch> revenueByBranch,
        RevenueGrowth revenueGrowth
    ) {}
    
    public record RevenueByBranch(
        Long branchId,
        String branchName,
        Double revenue,
        Double percentage
    ) {}
    
    public record RevenueGrowth(
        Double daily,
        Double weekly,
        Double monthly,
        Double yearly
    ) {}
    
    public record CostAnalysis(
        Double operationalCosts,
        Double sellerCommissions,
        Double systemMaintenance,
        Double netProfit,
        Double profitMargin
    ) {}
}
