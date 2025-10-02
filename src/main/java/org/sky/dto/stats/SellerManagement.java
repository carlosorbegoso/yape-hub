package org.sky.dto.stats;
public record SellerManagement(
    SellerOverview sellerOverview,

    SellerPerformanceDistribution sellerPerformanceDistribution,

SellerActivity sellerActivity
) {}