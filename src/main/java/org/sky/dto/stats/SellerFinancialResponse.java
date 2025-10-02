package org.sky.dto.stats;

public record SellerFinancialResponse(
    Long sellerId,
    String sellerName,
    double totalSales,
    String currency,
    double commissionRate,
    double commissionAmount,
    double netEarnings,
    Period period,
    String include,
    Integer transactions,
    Long confirmedTransactions,
    double averageTransactionValue
) {
    public record Period(
        String start,
        String end
    ) {}
}
