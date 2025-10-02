package org.sky.dto.stats.seller;

import org.sky.dto.stats.SellerActivity;
import org.sky.dto.stats.SellerOverview;
import org.sky.dto.stats.SellerPerformanceDistribution;

public record SellerManagementResponse(
    SellerOverview sellerOverview,
    SellerPerformanceDistribution sellerPerformanceDistribution,
    SellerActivity sellerActivity
) {



}
