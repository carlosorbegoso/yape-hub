package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerFinancialSpecificMetrics(
    Double totalSales,
    Double commissionRate,
    Double commissionAmount,
    Double netAmount,
    String currency
) {}
