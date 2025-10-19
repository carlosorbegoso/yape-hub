package org.sky.dto.response.seller;


import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerManagementResponse(
    SellerOverview sellerOverview,
    SellerPerformanceDistribution sellerPerformanceDistribution,
    SellerActivity sellerActivity
) {}
