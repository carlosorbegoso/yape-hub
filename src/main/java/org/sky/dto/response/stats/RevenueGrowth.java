package org.sky.dto.response.stats;

public record RevenueGrowth(
    Double daily,
    Double weekly,
    Double monthly,
    Double yearly
) {}