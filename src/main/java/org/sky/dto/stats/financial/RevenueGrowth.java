package org.sky.dto.stats.financial;

public record RevenueGrowth(
    Double daily,
    Double weekly,
    Double monthly,
    Double yearly
) {}