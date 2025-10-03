package org.sky.dto.response.seller;


public record SellerManagementResponse(
    SellerOverview sellerOverview,
    SellerPerformanceDistribution sellerPerformanceDistribution,
    SellerActivity sellerActivity
) {}
