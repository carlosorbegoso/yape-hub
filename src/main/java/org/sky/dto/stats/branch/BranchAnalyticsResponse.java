package org.sky.dto.stats.branch;

import java.util.List;

public record BranchAnalyticsResponse(
    List<BranchPerformanceData> branchPerformance,
    BranchComparison branchComparison
) {
    
    public record BranchPerformanceData(
        Long branchId,
        String branchName,
        String branchCode,
        Double totalSales,
        Long totalTransactions,
        Long activeSellers,
        Long inactiveSellers,
        Double averageSalesPerSeller,
        Double performanceScore,
        Double growthRate,
        String lastActivity
    ) {}
    
    public record BranchComparison(
        BranchSummary topPerformingBranch,
        BranchSummary lowestPerformingBranch,
        AverageBranchPerformance averageBranchPerformance
    ) {}
    
    public record BranchSummary(
        String branchName,
        Double totalSales,
        Long totalTransactions,
        Double performanceScore
    ) {}
    
    public record AverageBranchPerformance(
        Double averageSales,
        Double averageTransactions,
        Double averagePerformanceScore
    ) {}
}
