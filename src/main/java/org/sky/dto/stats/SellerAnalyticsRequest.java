package org.sky.dto.stats;

import java.time.LocalDate;

public record SellerAnalyticsRequest(
    Long sellerId,
    LocalDate startDate,
    LocalDate endDate,
    String include,
    String period,
    String metric,
    String granularity,
    Double confidence,
    Integer days
) {}