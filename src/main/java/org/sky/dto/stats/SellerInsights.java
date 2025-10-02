package org.sky.dto.stats;

public record SellerInsights(
    String peakPerformanceDay,
    String peakPerformanceHour,
    Double averageTransactionValue,
    Double customerRetentionRate,
    Double repeatCustomerRate,
    Double newCustomerRate,
    Double conversionRate,
    Double satisfactionScore
) {}