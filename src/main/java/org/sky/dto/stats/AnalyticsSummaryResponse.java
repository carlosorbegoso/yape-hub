package org.sky.dto.stats;

import org.sky.dto.stats.admin.AdministrativeInsightsResponse;
import org.sky.dto.stats.branch.BranchAnalyticsResponse;
import org.sky.dto.stats.financial.FinancialOverviewResponse;
import org.sky.dto.stats.security.ComplianceAndSecurityResponse;
import org.sky.dto.stats.seller.SellerManagementResponse;
import org.sky.dto.stats.system.SystemMetricsResponse;

import java.util.List;

public record AnalyticsSummaryResponse(

    OverviewMetrics overview,
    

    List<DailySalesData> dailySales,
    

    List<HourlySalesData> hourlySales,
    

    List<WeeklySalesData> weeklySales,
    

    List<MonthlySalesData> monthlySales,
    

    List<TopSellerData> topSellers,
    

    PerformanceMetrics performanceMetrics,
    

    SellerGoals sellerGoals,
    

    SellerPerformance sellerPerformance,
    

    SellerComparisons sellerComparisons,
    

    SellerTrends sellerTrends,
    

    SellerAchievements sellerAchievements,
    

    SellerInsights sellerInsights,
    

    SellerForecasting sellerForecasting,
    

    SellerAnalytics sellerAnalytics,
    

    BranchAnalyticsResponse branchAnalytics,
    

    SellerManagementResponse sellerManagement,
    

   SystemMetricsResponse systemMetrics,
    

   AdministrativeInsightsResponse administrativeInsights,
    

  FinancialOverviewResponse financialOverview,
    

  ComplianceAndSecurityResponse complianceAndSecurity
) {

}
