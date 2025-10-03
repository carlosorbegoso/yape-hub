package org.sky.dto.response.stats;

public record DailyStats(
    String date,
    Double totalSales,
    Long transactionCount,
    Double averageValue
) {}
