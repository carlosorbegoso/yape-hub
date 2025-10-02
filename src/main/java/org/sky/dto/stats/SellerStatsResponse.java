package org.sky.dto.stats;

import java.util.List;

public record SellerStatsResponse(
    Long sellerId,
    String sellerName,
    PeriodInfo period,
    SellerSummaryStats summary,
    List<DailyStats> dailyStats
) {
    public record PeriodInfo(
        String startDate,
        String endDate,
        int totalDays
    ) {}
    
    public record SellerSummaryStats(
        Double totalSales,
        Long totalTransactions,
        Double averageTransactionValue,
        Long pendingPayments,
        Long confirmedPayments,
        Long rejectedPayments,
        Double claimRate
    ) {}
    
    public record DailyStats(
        String date,
        Double totalSales,
        Long transactionCount,
        Double averageValue,
        Long pendingCount,
        Long confirmedCount
    ) {}
}
