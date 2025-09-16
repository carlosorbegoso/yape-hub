package org.sky.dto.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QuickSummaryResponse(
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
    Double averageGrowth,
    
    @JsonProperty("pendingPayments")
    Long pendingPayments,
    
    @JsonProperty("confirmedPayments")
    Long confirmedPayments,
    
    @JsonProperty("rejectedPayments")
    Long rejectedPayments,
    
    @JsonProperty("claimRate")
    Double claimRate,
    
    @JsonProperty("averageConfirmationTime")
    Double averageConfirmationTime
) {}
