package org.sky.dto.stats.financial;

import java.util.List;

public record RevenueBreakdown(
    Double totalRevenue,
    List<RevenueByBranch> revenueByBranch,
    RevenueGrowth revenueGrowth
) {}
