package org.sky.dto.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SellerStatsResponse(
    @JsonProperty("sellerId")
    Long sellerId,
    
    @JsonProperty("sellerName")
    String sellerName,
    
    @JsonProperty("period")
    PeriodInfo period,
    
    @JsonProperty("summary")
    SellerSummaryStats summary,
    
    @JsonProperty("dailyStats")
    List<DailyStats> dailyStats
) {
    public record PeriodInfo(
        @JsonProperty("startDate")
        String startDate,
        
        @JsonProperty("endDate")
        String endDate,
        
        @JsonProperty("totalDays")
        int totalDays
    ) {}
    
    public record SellerSummaryStats(
        @JsonProperty("totalSales")
        Double totalSales,
        
        @JsonProperty("totalTransactions")
        Long totalTransactions,
        
        @JsonProperty("averageTransactionValue")
        Double averageTransactionValue,
        
        @JsonProperty("pendingPayments")
        Long pendingPayments,
        
        @JsonProperty("confirmedPayments")
        Long confirmedPayments,
        
        @JsonProperty("rejectedPayments")
        Long rejectedPayments,
        
        @JsonProperty("claimRate")
        Double claimRate
    ) {}
    
    public record DailyStats(
        @JsonProperty("date")
        String date,
        
        @JsonProperty("totalSales")
        Double totalSales,
        
        @JsonProperty("transactionCount")
        Long transactionCount,
        
        @JsonProperty("averageValue")
        Double averageValue,
        
        @JsonProperty("pendingCount")
        Long pendingCount,
        
        @JsonProperty("confirmedCount")
        Long confirmedCount
    ) {}
}
