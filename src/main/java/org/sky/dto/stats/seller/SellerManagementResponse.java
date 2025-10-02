package org.sky.dto.stats.seller;

public record SellerManagementResponse(
    SellerOverview sellerOverview,
    SellerPerformanceDistribution sellerPerformanceDistribution,
    SellerActivity sellerActivity
) {
    
    public record SellerOverview(
        Long totalSellers,
        Long activeSellers,
        Long inactiveSellers,
        Long newSellersThisMonth,
        Long sellersWithZeroSales,
        Long topPerformers,
        Long underPerformers
    ) {}
    
    public record SellerPerformanceDistribution(
        Long excellent,
        Long good,
        Long average,
        Long poor
    ) {}
    
    public record SellerActivity(
        Long dailyActiveSellers,
        Long weeklyActiveSellers,
        Long monthlyActiveSellers,
        Double averageSessionDuration,
        Double averageTransactionsPerSeller
    ) {}
}
