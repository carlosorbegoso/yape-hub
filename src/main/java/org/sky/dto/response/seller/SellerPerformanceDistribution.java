package org.sky.dto.response.seller;

public record SellerPerformanceDistribution(
    Long excellent,
    Long good,
    Long average,
    Long poor
) {
    public static SellerPerformanceDistribution empty() {
        return new SellerPerformanceDistribution(0L, 0L, 0L, 0L);
    }
}