package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record DailyStats(
    String date,
    Double totalSales,
    Long transactionCount,
    Double averageValue
) {}
