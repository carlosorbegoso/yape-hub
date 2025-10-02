package org.sky.dto.stats;

import java.time.LocalDate;

public record FinancialAnalyticsRequest(
    Long adminId,
    LocalDate startDate,
    LocalDate endDate,
    String include,
    String currency,
    Double commissionRate,
    Double taxRate
) {}