package org.sky.dto.request.stats;

import java.time.LocalDate;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record FinancialAnalyticsRequest(
    Long adminId,
    LocalDate startDate,
    LocalDate endDate,
    String include,
    String currency,
    Double commissionRate,
    Double taxRate
) {}
