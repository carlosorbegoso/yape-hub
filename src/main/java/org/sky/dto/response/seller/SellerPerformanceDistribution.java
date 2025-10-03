package org.sky.dto.response.seller;

public record SellerPerformanceDistribution(
    Long excellent,
    Long good,
    Long average,
    Long poor
) {}