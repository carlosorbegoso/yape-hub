package org.sky.dto.response.seller;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerPerformance(
    String bestDay,
    String worstDay,
    Double averageDailySales,
    Double consistencyScore,
    List<String> peakPerformanceHours,
    Double productivityScore,
    Double efficiencyRate,
    Double responseTime
) {
    public static SellerPerformance empty() {
        return new SellerPerformance("", "", 0.0, 0.0, List.of(), 0.0, 0.0, 0.0);
    }
}
