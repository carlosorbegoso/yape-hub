package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerOverview(
    Long totalSellers,
    Long activeSellers,
    Long inactiveSellers,
    Long newSellersThisMonth,
    Long sellersWithZeroSales,
    Long topPerformers,
    Long underPerformers
) {
    public static SellerOverview empty() {
        return new SellerOverview(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }
}
