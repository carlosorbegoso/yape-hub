package org.sky.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
    SummaryInfo summary,
    List<DailyStats> dailyStats,
    List<TopSeller> topSellers,
    List<BranchStats> branchStats
) {
    public record SummaryInfo(
        int totalTransactions,
        BigDecimal totalAmount,
        int pendingTransactions,
        int activeSellers,
        int totalBranches
    ) {}
    
    public record DailyStats(
        String date,
        int transactions,
        BigDecimal amount
    ) {}
    
    public record TopSeller(
        Long sellerId,
        String sellerName,
        int transactions,
        BigDecimal amount
    ) {}
    
    public record BranchStats(
        Long branchId,
        String branchName,
        int transactions,
        BigDecimal amount
    ) {}
}
