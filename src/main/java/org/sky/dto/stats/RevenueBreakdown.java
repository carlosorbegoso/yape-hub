package org.sky.dto.stats;

import java.util.List;

public record RevenueBreakdown(
    Double totalRevenue,
    List<RevenueByBranch> revenueByBranch,
    RevenueGrowth revenueGrowth
) {}