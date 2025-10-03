package org.sky.dto.response.stats;

import java.util.List;

public record RevenueBreakdown(
    Double totalRevenue,
    List<RevenueByBranch> revenueByBranch,
    RevenueGrowth revenueGrowth
) {}