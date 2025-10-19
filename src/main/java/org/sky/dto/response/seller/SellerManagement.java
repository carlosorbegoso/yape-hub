package org.sky.dto.response.seller;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerManagement(
    SellerOverview sellerOverview,
    SellerPerformanceDistribution sellerPerformanceDistribution,
    SellerActivity sellerActivity
) {
    public static SellerManagement empty() {
        return new SellerManagement(
            SellerOverview.empty(),
            SellerPerformanceDistribution.empty(),
            SellerActivity.empty()
        );
    }
}
