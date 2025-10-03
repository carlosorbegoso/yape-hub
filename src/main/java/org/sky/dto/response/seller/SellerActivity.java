package org.sky.dto.response.seller;

public record SellerActivity(
    Long dailyActiveSellers,
    Long weeklyActiveSellers,
    Long monthlyActiveSellers,
    Double averageSessionDuration,
    Double averageTransactionsPerSeller
) {}