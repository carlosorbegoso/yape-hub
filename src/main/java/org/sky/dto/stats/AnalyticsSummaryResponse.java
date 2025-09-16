package org.sky.dto.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AnalyticsSummaryResponse(
    @JsonProperty("overview")
    OverviewMetrics overview,
    
    @JsonProperty("dailySales")
    List<DailySalesData> dailySales,
    
    @JsonProperty("topSellers")
    List<TopSellerData> topSellers,
    
    @JsonProperty("performanceMetrics")
    PerformanceMetrics performanceMetrics
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
}
