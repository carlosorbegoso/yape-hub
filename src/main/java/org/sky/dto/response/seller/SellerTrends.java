package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerTrends(
    String salesTrend,
    String transactionTrend,
    Double growthRate,
    String momentum,
    String trendDirection,
    Double volatility,
    String seasonality
) {
    public static SellerTrends empty() {
        return new SellerTrends("", "", 0.0, "", "", 0.0, "");
    }
}
