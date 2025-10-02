package org.sky.dto.stats;

public record RevenueGrowth(
    Double daily,
    Double weekly,
    Double monthly,
    Double yearly
) {}