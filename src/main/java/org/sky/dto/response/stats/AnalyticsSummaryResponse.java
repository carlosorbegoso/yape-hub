package org.sky.dto.response.stats;

import org.sky.dto.response.admin.AdministrativeInsights;
import org.sky.dto.response.admin.ComplianceAndSecurity;
import org.sky.dto.response.branch.BranchAnalytics;
import org.sky.dto.response.seller.*;

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
    

    BranchAnalytics branchAnalytics,
    

    SellerManagement sellerManagement,
    

   SystemMetrics systemMetrics,
    

   AdministrativeInsights administrativeInsights,
    

  FinancialOverview financialOverview,
    

  ComplianceAndSecurity complianceAndSecurity
) {

}
