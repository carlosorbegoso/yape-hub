package org.sky.dto.stats;

import java.util.List;

public record SellerFinancialResponse(
    Long sellerId,
    Double totalRevenue,
    Double totalCommissions,
    Double netProfit,
    Integer totalTransactions,
    Double averageTransactionValue,
    List<DailySalesData> dailySales,
    Double commissionRate,
    Double profitMargin,
    Period period
) {
    public record Period(
        String startDate,
        String endDate
    ) {}
}