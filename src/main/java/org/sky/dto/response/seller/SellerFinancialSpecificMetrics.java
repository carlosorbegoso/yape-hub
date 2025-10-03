package org.sky.dto.response.seller;

public record SellerFinancialSpecificMetrics(
    Double totalSales,
    Double commissionRate,
    Double commissionAmount,
    Double netAmount,
    String currency
) {}