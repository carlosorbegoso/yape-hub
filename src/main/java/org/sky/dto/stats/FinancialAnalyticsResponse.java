package org.sky.dto.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinancialAnalyticsResponse(
    @JsonProperty("totalRevenue")
    Double totalRevenue,
    
    @JsonProperty("currency")
    String currency,
    
    @JsonProperty("taxRate")
    Double taxRate,
    
    @JsonProperty("taxAmount")
    Double taxAmount,
    
    @JsonProperty("netRevenue")
    Double netRevenue,
    
    @JsonProperty("period")
    Period period,
    
    @JsonProperty("include")
    String include,
    
    @JsonProperty("transactions")
    Long transactions,
    
    @JsonProperty("confirmedTransactions")
    Long confirmedTransactions,
    
    @JsonProperty("averageTransactionValue")
    Double averageTransactionValue
) {
    public record Period(
        @JsonProperty("start")
        String start,
        
        @JsonProperty("end")
        String end
    ) {}
}

