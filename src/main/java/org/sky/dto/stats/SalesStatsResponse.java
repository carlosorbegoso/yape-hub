package org.sky.dto.stats;

import java.util.List;

public record SalesStatsResponse(
    PeriodInfo period,
    SummaryStats summary,
    List<DailyStats> dailyStats,
    List<SellerStats> sellerStats
) {
    public record PeriodInfo(
        String startDate,
        String endDate,
        int totalDays
    ) {}
    
    public record SummaryStats(
        Double totalSales,
        Long totalTransactions,
        Double averageTransactionValue,
        Long pendingPayments,
        Long confirmedPayments,
        Long rejectedPayments
    ) {}
    
    public record DailyStats(
        String date,
        Double totalSales,
        Long transactionCount,
        Double averageValue
    ) {}
    
    public record SellerStats(
        Long sellerId,
        String sellerName,
        Double totalSales,
        Long transactionCount,
        Double averageValue,
        Long pendingCount
    ) {}
}
