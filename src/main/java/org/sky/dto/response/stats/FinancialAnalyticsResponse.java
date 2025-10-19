package org.sky.dto.response.stats;

import org.sky.dto.response.common.Period;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
) {}

