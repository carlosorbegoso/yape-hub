package org.sky.dto.response.stats;

import org.sky.dto.response.seller.*;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
