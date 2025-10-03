package org.sky.dto.response.seller;
public record SellerManagement(
    SellerOverview sellerOverview,

    SellerPerformanceDistribution sellerPerformanceDistribution,

SellerActivity sellerActivity
) {}