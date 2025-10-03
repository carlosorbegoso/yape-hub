package org.sky.dto.response.stats;

import java.util.List;

public record SellerPerformanceData(
    String bestDay,
    String worstDay,
    Double averageDailySales,
    Double consistencyScore,
    List<String> peakPerformanceHours,
    Double productivityScore,
    Double efficiencyRate,
    Double responseTime
) {
    public static SellerPerformanceData empty() {
        return new SellerPerformanceData("", "", 0.0, 0.0, List.of(), 0.0, 0.0, 0.0);
    }
}
