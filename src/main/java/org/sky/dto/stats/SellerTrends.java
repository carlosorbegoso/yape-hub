package org.sky.dto.stats;

public record SellerTrends(
    String salesTrend,
    String transactionTrend,
    Double growthRate,
    String momentum,
    String trendDirection,
    Double volatility,
    String seasonality
) {}