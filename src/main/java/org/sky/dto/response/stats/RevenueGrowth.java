package org.sky.dto.response.stats;

public record RevenueGrowth(
    Double daily,
    Double weekly,
    Double monthly,
    Double yearly
) {
    public static RevenueGrowth empty() {
        return new RevenueGrowth(0.0, 0.0, 0.0, 0.0);
    }
}