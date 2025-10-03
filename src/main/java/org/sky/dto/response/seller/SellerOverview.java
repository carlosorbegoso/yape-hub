package org.sky.dto.response.seller;

public record SellerOverview(
    Long totalSellers,
    Long activeSellers,
    Long inactiveSellers,
    Long newSellersThisMonth,
    Long sellersWithZeroSales,
    Long topPerformers,
    Long underPerformers
) {}