package org.sky.dto.stats;

public record SellerActivity(
    Long dailyActiveSellers,
    Long weeklyActiveSellers,
    Long monthlyActiveSellers,
    Double averageSessionDuration,
    Double averageTransactionsPerSeller
) {}