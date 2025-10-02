package org.sky.dto.stats;

public record PaymentTransparencyResponse(
    Period period,
    Double totalRevenue,
    Long totalTransactions,
    Long confirmedTransactions,
    Double processingFees,
    Double platformFees,
    Double taxRate,
    Double taxAmount,
    Double sellerCommissionRate,
    Double sellerCommissionAmount,
    Double transparencyScore,
    String lastUpdated
) {
    public record Period(
        String start,
        String end
    ) {}
}

