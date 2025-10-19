package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
