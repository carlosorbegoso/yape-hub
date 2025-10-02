package org.sky.dto.stats;

public record SellerPerformanceDistribution(
    Long excellent,
    Long good,
    Long average,
    Long poor
) {}