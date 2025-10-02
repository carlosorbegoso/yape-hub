package org.sky.dto.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AnalyticsSummaryResponse(
    @JsonProperty("overview")
    OverviewMetrics overview,
    
    @JsonProperty("dailySales")
    List<DailySalesData> dailySales,
    
    @JsonProperty("hourlySales")
    List<HourlySalesData> hourlySales,
    
    @JsonProperty("weeklySales")
    List<WeeklySalesData> weeklySales,
    
    @JsonProperty("monthlySales")
    List<MonthlySalesData> monthlySales,
    
    @JsonProperty("topSellers")
    List<TopSellerData> topSellers,
    
    @JsonProperty("performanceMetrics")
    PerformanceMetrics performanceMetrics,
    
    @JsonProperty("sellerGoals")
    SellerGoals sellerGoals,
    
    @JsonProperty("sellerPerformance")
    SellerPerformance sellerPerformance,
    
    @JsonProperty("sellerComparisons")
    SellerComparisons sellerComparisons,
    
    @JsonProperty("sellerTrends")
    SellerTrends sellerTrends,
    
    @JsonProperty("sellerAchievements")
    SellerAchievements sellerAchievements,
    
    @JsonProperty("sellerInsights")
    SellerInsights sellerInsights,
    
    @JsonProperty("sellerForecasting")
    SellerForecasting sellerForecasting,
    
    @JsonProperty("sellerAnalytics")
    SellerAnalytics sellerAnalytics,
    
    @JsonProperty("branchAnalytics")
    org.sky.dto.stats.branch.BranchAnalyticsResponse branchAnalytics,
    
    @JsonProperty("sellerManagement")
    org.sky.dto.stats.seller.SellerManagementResponse sellerManagement,
    
    @JsonProperty("systemMetrics")
    org.sky.dto.stats.system.SystemMetricsResponse systemMetrics,
    
    @JsonProperty("administrativeInsights")
    org.sky.dto.stats.admin.AdministrativeInsightsResponse administrativeInsights,
    
    @JsonProperty("financialOverview")
    org.sky.dto.stats.financial.FinancialOverviewResponse financialOverview,
    
    @JsonProperty("complianceAndSecurity")
    org.sky.dto.stats.security.ComplianceAndSecurityResponse complianceAndSecurity
) {
    public record OverviewMetrics(
        @JsonProperty("totalSales")
        Double totalSales,
        
        @JsonProperty("totalTransactions")
        Long totalTransactions,
        
        @JsonProperty("averageTransactionValue")
        Double averageTransactionValue,
        
        @JsonProperty("salesGrowth")
        Double salesGrowth,
        
        @JsonProperty("transactionGrowth")
        Double transactionGrowth,
        
        @JsonProperty("averageGrowth")
        Double averageGrowth
    ) {}
    
    public record DailySalesData(
        @JsonProperty("date")
        String date,
        
        @JsonProperty("dayName")
        String dayName,
        
        @JsonProperty("sales")
        Double sales,
        
        @JsonProperty("transactions")
        Long transactions
    ) {}
    
    public record TopSellerData(
        @JsonProperty("rank")
        Integer rank,
        
        @JsonProperty("sellerId")
        Long sellerId,
        
        @JsonProperty("sellerName")
        String sellerName,
        
        @JsonProperty("branchName")
        String branchName,
        
        @JsonProperty("totalSales")
        Double totalSales,
        
        @JsonProperty("transactionCount")
        Long transactionCount
    ) {}
    
    public record PerformanceMetrics(
        @JsonProperty("averageConfirmationTime")
        Double averageConfirmationTime,
        
        @JsonProperty("claimRate")
        Double claimRate,
        
        @JsonProperty("rejectionRate")
        Double rejectionRate,
        
        @JsonProperty("pendingPayments")
        Long pendingPayments,
        
        @JsonProperty("confirmedPayments")
        Long confirmedPayments,
        
        @JsonProperty("rejectedPayments")
        Long rejectedPayments
    ) {}
    
    public record HourlySalesData(
        @JsonProperty("hour")
        String hour,
        
        @JsonProperty("sales")
        Double sales,
        
        @JsonProperty("transactions")
        Long transactions
    ) {}
    
    public record WeeklySalesData(
        @JsonProperty("week")
        String week,
        
        @JsonProperty("sales")
        Double sales,
        
        @JsonProperty("transactions")
        Long transactions
    ) {}
    
    public record MonthlySalesData(
        @JsonProperty("month")
        String month,
        
        @JsonProperty("sales")
        Double sales,
        
        @JsonProperty("transactions")
        Long transactions
    ) {}
    
    public record SellerGoals(
        @JsonProperty("dailyTarget")
        Double dailyTarget,
        
        @JsonProperty("weeklyTarget")
        Double weeklyTarget,
        
        @JsonProperty("monthlyTarget")
        Double monthlyTarget,
        
        @JsonProperty("yearlyTarget")
        Double yearlyTarget,
        
        @JsonProperty("achievementRate")
        Double achievementRate,
        
        @JsonProperty("dailyProgress")
        Double dailyProgress,
        
        @JsonProperty("weeklyProgress")
        Double weeklyProgress,
        
        @JsonProperty("monthlyProgress")
        Double monthlyProgress
    ) {}
    
    public record SellerPerformance(
        @JsonProperty("bestDay")
        String bestDay,
        
        @JsonProperty("worstDay")
        String worstDay,
        
        @JsonProperty("averageDailySales")
        Double averageDailySales,
        
        @JsonProperty("consistencyScore")
        Double consistencyScore,
        
        @JsonProperty("peakPerformanceHours")
        List<String> peakPerformanceHours,
        
        @JsonProperty("productivityScore")
        Double productivityScore,
        
        @JsonProperty("efficiencyRate")
        Double efficiencyRate,
        
        @JsonProperty("responseTime")
        Double responseTime
    ) {}
    
    public record SellerComparisons(
        @JsonProperty("vsPreviousWeek")
        ComparisonData vsPreviousWeek,
        
        @JsonProperty("vsPreviousMonth")
        ComparisonData vsPreviousMonth,
        
        @JsonProperty("vsPersonalBest")
        ComparisonData vsPersonalBest,
        
        @JsonProperty("vsAverage")
        ComparisonData vsAverage
    ) {}
    
    public record ComparisonData(
        @JsonProperty("salesChange")
        Double salesChange,
        
        @JsonProperty("transactionChange")
        Long transactionChange,
        
        @JsonProperty("percentageChange")
        Double percentageChange
    ) {}
    
    public record SellerTrends(
        @JsonProperty("salesTrend")
        String salesTrend,
        
        @JsonProperty("transactionTrend")
        String transactionTrend,
        
        @JsonProperty("growthRate")
        Double growthRate,
        
        @JsonProperty("momentum")
        String momentum,
        
        @JsonProperty("trendDirection")
        String trendDirection,
        
        @JsonProperty("volatility")
        Double volatility,
        
        @JsonProperty("seasonality")
        String seasonality
    ) {}
    
    public record SellerAchievements(
        @JsonProperty("streakDays")
        Long streakDays,
        
        @JsonProperty("bestStreak")
        Long bestStreak,
        
        @JsonProperty("totalStreaks")
        Long totalStreaks,
        
        @JsonProperty("milestones")
        List<Milestone> milestones,
        
        @JsonProperty("badges")
        List<Badge> badges
    ) {}
    
    public record Milestone(
        @JsonProperty("type")
        String type,
        
        @JsonProperty("date")
        String date,
        
        @JsonProperty("achieved")
        Boolean achieved,
        
        @JsonProperty("value")
        Double value
    ) {}
    
    public record Badge(
        @JsonProperty("name")
        String name,
        
        @JsonProperty("icon")
        String icon,
        
        @JsonProperty("description")
        String description,
        
        @JsonProperty("earned")
        Boolean earned,
        
        @JsonProperty("date")
        String date
    ) {}
    
    public record SellerInsights(
        @JsonProperty("peakPerformanceDay")
        String peakPerformanceDay,
        
        @JsonProperty("peakPerformanceHour")
        String peakPerformanceHour,
        
        @JsonProperty("averageTransactionValue")
        Double averageTransactionValue,
        
        @JsonProperty("customerRetentionRate")
        Double customerRetentionRate,
        
        @JsonProperty("repeatCustomerRate")
        Double repeatCustomerRate,
        
        @JsonProperty("newCustomerRate")
        Double newCustomerRate,
        
        @JsonProperty("conversionRate")
        Double conversionRate,
        
        @JsonProperty("satisfactionScore")
        Double satisfactionScore
    ) {}
    
    public record SellerForecasting(
        @JsonProperty("predictedSales")
        List<PredictedSale> predictedSales,
        
        @JsonProperty("trendAnalysis")
        TrendAnalysis trendAnalysis,
        
        @JsonProperty("recommendations")
        List<String> recommendations
    ) {}
    
    public record PredictedSale(
        @JsonProperty("date")
        String date,
        
        @JsonProperty("predicted")
        Double predicted,
        
        @JsonProperty("confidence")
        Double confidence
    ) {}
    
    public record TrendAnalysis(
        @JsonProperty("trend")
        String trend,
        
        @JsonProperty("slope")
        Double slope,
        
        @JsonProperty("r2")
        Double r2,
        
        @JsonProperty("forecastAccuracy")
        Double forecastAccuracy
    ) {}
    
    public record SellerAnalytics(
        @JsonProperty("salesDistribution")
        SalesDistribution salesDistribution,
        
        @JsonProperty("transactionPatterns")
        TransactionPatterns transactionPatterns,
        
        @JsonProperty("performanceIndicators")
        PerformanceIndicators performanceIndicators
    ) {}
    
    public record SalesDistribution(
        @JsonProperty("weekday")
        Double weekday,
        
        @JsonProperty("weekend")
        Double weekend,
        
        @JsonProperty("morning")
        Double morning,
        
        @JsonProperty("afternoon")
        Double afternoon,
        
        @JsonProperty("evening")
        Double evening
    ) {}
    
    public record TransactionPatterns(
        @JsonProperty("averageTransactionsPerDay")
        Double averageTransactionsPerDay,
        
        @JsonProperty("mostActiveDay")
        String mostActiveDay,
        
        @JsonProperty("mostActiveHour")
        String mostActiveHour,
        
        @JsonProperty("transactionFrequency")
        String transactionFrequency
    ) {}
    
    public record PerformanceIndicators(
        @JsonProperty("salesVelocity")
        Double salesVelocity,
        
        @JsonProperty("transactionVelocity")
        Double transactionVelocity,
        
        @JsonProperty("efficiencyIndex")
        Double efficiencyIndex,
        
        @JsonProperty("consistencyIndex")
        Double consistencyIndex
    ) {}
    
    // Branch Analytics
    public record BranchAnalytics(
        @JsonProperty("branchPerformance")
        List<BranchPerformanceData> branchPerformance,
        
        @JsonProperty("branchComparison")
        BranchComparison branchComparison
    ) {}
    
    public record BranchPerformanceData(
        @JsonProperty("branchId")
        Long branchId,
        
        @JsonProperty("branchName")
        String branchName,
        
        @JsonProperty("branchCode")
        String branchCode,
        
        @JsonProperty("totalSales")
        Double totalSales,
        
        @JsonProperty("totalTransactions")
        Long totalTransactions,
        
        @JsonProperty("activeSellers")
        Long activeSellers,
        
        @JsonProperty("inactiveSellers")
        Long inactiveSellers,
        
        @JsonProperty("averageSalesPerSeller")
        Double averageSalesPerSeller,
        
        @JsonProperty("performanceScore")
        Double performanceScore,
        
        @JsonProperty("growthRate")
        Double growthRate,
        
        @JsonProperty("lastActivity")
        String lastActivity
    ) {}
    
    public record BranchComparison(
        @JsonProperty("topPerformingBranch")
        BranchSummary topPerformingBranch,
        
        @JsonProperty("lowestPerformingBranch")
        BranchSummary lowestPerformingBranch,
        
        @JsonProperty("averageBranchPerformance")
        AverageBranchPerformance averageBranchPerformance
    ) {}
    
    public record BranchSummary(
        @JsonProperty("branchId")
        Long branchId,
        
        @JsonProperty("branchName")
        String branchName,
        
        @JsonProperty("sales")
        Double sales,
        
        @JsonProperty("growth")
        Double growth
    ) {}
    
    public record AverageBranchPerformance(
        @JsonProperty("sales")
        Double sales,
        
        @JsonProperty("transactions")
        Long transactions,
        
        @JsonProperty("sellers")
        Long sellers
    ) {}
    
    // Seller Management
    public record SellerManagement(
        @JsonProperty("sellerOverview")
        SellerOverview sellerOverview,
        
        @JsonProperty("sellerPerformanceDistribution")
        SellerPerformanceDistribution sellerPerformanceDistribution,
        
        @JsonProperty("sellerActivity")
        SellerActivity sellerActivity
    ) {}
    
    public record SellerOverview(
        @JsonProperty("totalSellers")
        Long totalSellers,
        
        @JsonProperty("activeSellers")
        Long activeSellers,
        
        @JsonProperty("inactiveSellers")
        Long inactiveSellers,
        
        @JsonProperty("newSellersThisMonth")
        Long newSellersThisMonth,
        
        @JsonProperty("sellersWithZeroSales")
        Long sellersWithZeroSales,
        
        @JsonProperty("topPerformers")
        Long topPerformers,
        
        @JsonProperty("underPerformers")
        Long underPerformers
    ) {}
    
    public record SellerPerformanceDistribution(
        @JsonProperty("excellent")
        Long excellent,
        
        @JsonProperty("good")
        Long good,
        
        @JsonProperty("average")
        Long average,
        
        @JsonProperty("poor")
        Long poor
    ) {}
    
    public record SellerActivity(
        @JsonProperty("dailyActiveSellers")
        Long dailyActiveSellers,
        
        @JsonProperty("weeklyActiveSellers")
        Long weeklyActiveSellers,
        
        @JsonProperty("monthlyActiveSellers")
        Long monthlyActiveSellers,
        
        @JsonProperty("averageSessionDuration")
        Double averageSessionDuration,
        
        @JsonProperty("averageTransactionsPerSeller")
        Double averageTransactionsPerSeller
    ) {}
    
    // System Metrics
    public record SystemMetrics(
        @JsonProperty("overallSystemHealth")
        OverallSystemHealth overallSystemHealth,
        
        @JsonProperty("paymentSystemMetrics")
        PaymentSystemMetrics paymentSystemMetrics,
        
        @JsonProperty("userEngagement")
        UserEngagement userEngagement
    ) {}
    
    public record OverallSystemHealth(
        @JsonProperty("totalSystemSales")
        Double totalSystemSales,
        
        @JsonProperty("totalSystemTransactions")
        Long totalSystemTransactions,
        
        @JsonProperty("systemUptime")
        Double systemUptime,
        
        @JsonProperty("averageResponseTime")
        Double averageResponseTime,
        
        @JsonProperty("errorRate")
        Double errorRate,
        
        @JsonProperty("activeUsers")
        Long activeUsers
    ) {}
    
    public record PaymentSystemMetrics(
        @JsonProperty("totalPaymentsProcessed")
        Long totalPaymentsProcessed,
        
        @JsonProperty("pendingPayments")
        Long pendingPayments,
        
        @JsonProperty("confirmedPayments")
        Long confirmedPayments,
        
        @JsonProperty("rejectedPayments")
        Long rejectedPayments,
        
        @JsonProperty("averageConfirmationTime")
        Double averageConfirmationTime,
        
        @JsonProperty("paymentSuccessRate")
        Double paymentSuccessRate
    ) {}
    
    public record UserEngagement(
        @JsonProperty("dailyActiveUsers")
        Long dailyActiveUsers,
        
        @JsonProperty("weeklyActiveUsers")
        Long weeklyActiveUsers,
        
        @JsonProperty("monthlyActiveUsers")
        Long monthlyActiveUsers,
        
        @JsonProperty("averageSessionDuration")
        Double averageSessionDuration,
        
        @JsonProperty("featureUsage")
        FeatureUsage featureUsage
    ) {}
    
    public record FeatureUsage(
        @JsonProperty("qrScanner")
        Double qrScanner,
        
        @JsonProperty("paymentManagement")
        Double paymentManagement,
        
        @JsonProperty("analytics")
        Double analytics,
        
        @JsonProperty("notifications")
        Double notifications
    ) {}
    
    // Administrative Insights
    public record AdministrativeInsights(
        @JsonProperty("managementAlerts")
        List<ManagementAlert> managementAlerts,
        
        @JsonProperty("recommendations")
        List<String> recommendations,
        
        @JsonProperty("growthOpportunities")
        GrowthOpportunities growthOpportunities
    ) {}
    
    public record ManagementAlert(
        @JsonProperty("type")
        String type,
        
        @JsonProperty("severity")
        String severity,
        
        @JsonProperty("message")
        String message,
        
        @JsonProperty("affectedBranch")
        String affectedBranch,
        
        @JsonProperty("affectedSellers")
        List<String> affectedSellers,
        
        @JsonProperty("recommendation")
        String recommendation
    ) {}
    
    public record GrowthOpportunities(
        @JsonProperty("potentialNewBranches")
        Long potentialNewBranches,
        
        @JsonProperty("marketExpansion")
        String marketExpansion,
        
        @JsonProperty("sellerRecruitment")
        Long sellerRecruitment,
        
        @JsonProperty("revenueProjection")
        Double revenueProjection
    ) {}
    
    // Financial Overview
    public record FinancialOverview(
        @JsonProperty("revenueBreakdown")
        RevenueBreakdown revenueBreakdown,
        
        @JsonProperty("costAnalysis")
        CostAnalysis costAnalysis
    ) {}
    
    public record RevenueBreakdown(
        @JsonProperty("totalRevenue")
        Double totalRevenue,
        
        @JsonProperty("revenueByBranch")
        List<RevenueByBranch> revenueByBranch,
        
        @JsonProperty("revenueGrowth")
        RevenueGrowth revenueGrowth
    ) {}
    
    public record RevenueByBranch(
        @JsonProperty("branchId")
        Long branchId,
        
        @JsonProperty("branchName")
        String branchName,
        
        @JsonProperty("revenue")
        Double revenue,
        
        @JsonProperty("percentage")
        Double percentage
    ) {}
    
    public record RevenueGrowth(
        @JsonProperty("daily")
        Double daily,
        
        @JsonProperty("weekly")
        Double weekly,
        
        @JsonProperty("monthly")
        Double monthly,
        
        @JsonProperty("yearly")
        Double yearly
    ) {}
    
    public record CostAnalysis(
        @JsonProperty("operationalCosts")
        Double operationalCosts,
        
        @JsonProperty("sellerCommissions")
        Double sellerCommissions,
        
        @JsonProperty("systemMaintenance")
        Double systemMaintenance,
        
        @JsonProperty("netProfit")
        Double netProfit,
        
        @JsonProperty("profitMargin")
        Double profitMargin
    ) {}
    
    // Compliance and Security
    public record ComplianceAndSecurity(
        @JsonProperty("securityMetrics")
        SecurityMetrics securityMetrics,
        
        @JsonProperty("complianceStatus")
        ComplianceStatus complianceStatus
    ) {}
    
    public record SecurityMetrics(
        @JsonProperty("failedLoginAttempts")
        Long failedLoginAttempts,
        
        @JsonProperty("suspiciousActivities")
        Long suspiciousActivities,
        
        @JsonProperty("dataBreaches")
        Long dataBreaches,
        
        @JsonProperty("securityScore")
        Double securityScore
    ) {}
    
    public record ComplianceStatus(
        @JsonProperty("dataProtection")
        String dataProtection,
        
        @JsonProperty("auditTrail")
        String auditTrail,
        
        @JsonProperty("backupStatus")
        String backupStatus,
        
        @JsonProperty("lastAudit")
        String lastAudit
    ) {}
}
