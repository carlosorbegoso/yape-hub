package org.sky.dto.stats;

public record FinancialAnalyticsResponse(
    Double totalRevenue,
    String currency,
    Double taxRate,
    Double taxAmount,
    Double netRevenue,
    Period period,
    String include,
    Long transactions,
    Long confirmedTransactions,
    Double averageTransactionValue
) {
    public record Period(
        String start,
        String end
    ) {}
}

