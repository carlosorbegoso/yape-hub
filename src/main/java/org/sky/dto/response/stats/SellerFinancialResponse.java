package org.sky.dto.response.stats;

import org.sky.dto.response.common.Period;

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
) {}