package org.sky.dto.request.stats;

import java.time.LocalDate;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
