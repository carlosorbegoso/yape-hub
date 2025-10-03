package org.sky.dto.response.seller;

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