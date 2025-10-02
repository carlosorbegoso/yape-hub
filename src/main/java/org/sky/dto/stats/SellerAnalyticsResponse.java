package org.sky.dto.stats;

import java.util.List;

public record SellerAnalyticsResponse(
    OverviewMetrics overview,
    List<DailySalesData> dailySales,
    List<HourlySalesData> hourlySales,
    List<WeeklySalesData> weeklySales,
    List<MonthlySalesData> monthlySales,
    PerformanceMetrics performanceMetrics,
    SellerGoals sellerGoals,
    SellerPerformance sellerPerformance,
    SellerComparisons sellerComparisons,
    SellerTrends sellerTrends,
    SellerAchievements sellerAchievements,
    SellerInsights sellerInsights,
    SellerForecasting sellerForecasting,
    SellerAnalytics sellerAnalytics
) {

}
