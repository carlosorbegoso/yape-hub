package org.sky.dto.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentTransparencyResponse(
    @JsonProperty("period")
    Period period,
    
    @JsonProperty("totalRevenue")
    Double totalRevenue,
    
    @JsonProperty("totalTransactions")
    Long totalTransactions,
    
    @JsonProperty("confirmedTransactions")
    Long confirmedTransactions,
    
    @JsonProperty("processingFees")
    Double processingFees,
    
    @JsonProperty("platformFees")
    Double platformFees,
    
    @JsonProperty("taxRate")
    Double taxRate,
    
    @JsonProperty("taxAmount")
    Double taxAmount,
    
    @JsonProperty("sellerCommissionRate")
    Double sellerCommissionRate,
    
    @JsonProperty("sellerCommissionAmount")
    Double sellerCommissionAmount,
    
    @JsonProperty("transparencyScore")
    Double transparencyScore,
    
    @JsonProperty("lastUpdated")
    String lastUpdated
) {
    public record Period(
        @JsonProperty("start")
        String start,
        
        @JsonProperty("end")
        String end
    ) {}
}

