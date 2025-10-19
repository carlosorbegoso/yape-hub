package org.sky.dto.response.stats;

import org.sky.dto.response.admin.AdministrativeInsights;
import org.sky.dto.response.admin.ComplianceAndSecurity;
import org.sky.dto.response.branch.BranchAnalytics;
import org.sky.dto.response.seller.*;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
/**
 * Respuesta completa de analytics para administrador
 */
public record AdminAnalyticsResponse(
    OverviewMetrics overview,
    List<DailySalesData> dailySales,
    List<TopSellerData> topSellers,
    PerformanceMetrics performanceMetrics,
    List<HourlySalesData> hourlySales,
    List<WeeklySalesData> weeklySales,
    List<MonthlySalesData> monthlySales,
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
    
    public static AdminAnalyticsResponse createComplete(
        OverviewMetrics overview,
        List<DailySalesData> dailySales,
        List<TopSellerData> topSellers,
        PerformanceMetrics performanceMetrics,
        List<HourlySalesData> hourlySales,
        List<WeeklySalesData> weeklySales,
        List<MonthlySalesData> monthlySales,
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
        ComplianceAndSecurity complianceAndSecurity) {
        
        return new AdminAnalyticsResponse(
            overview,
            dailySales != null ? dailySales : List.of(),
            topSellers != null ? topSellers : List.of(),
            performanceMetrics,
            hourlySales != null ? hourlySales : List.of(),
            weeklySales != null ? weeklySales : List.of(),
            monthlySales != null ? monthlySales : List.of(),
            sellerGoals,
            sellerPerformance,
            sellerComparisons,
            sellerTrends,
            sellerAchievements,
            sellerInsights,
            sellerForecasting,
            sellerAnalytics,
            branchAnalytics,
            sellerManagement,
            systemMetrics,
            administrativeInsights,
            financialOverview,
            complianceAndSecurity
        );
    }
    
    public static AdminAnalyticsResponse empty() {
        return createComplete(
            OverviewMetrics.empty(),
            List.of(), // dailySales
            List.of(), // topSellers
            PerformanceMetrics.empty(),
            List.of(), // hourlySales
            List.of(), // weeklySales
            List.of(), // monthlySales
            SellerGoals.empty(),
            SellerPerformance.empty(),
            SellerComparisons.empty(),
            SellerTrends.empty(),
            SellerAchievements.empty(),
            SellerInsights.empty(),
            SellerForecasting.empty(),
            SellerAnalytics.empty(),
            BranchAnalytics.empty(),
            SellerManagement.empty(),
            SystemMetrics.empty(),
            AdministrativeInsights.empty(),
            FinancialOverview.empty(),
            ComplianceAndSecurity.empty()
        );
    }
}
