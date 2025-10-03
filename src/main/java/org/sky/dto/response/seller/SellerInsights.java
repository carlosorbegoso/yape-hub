package org.sky.dto.response.seller;

public record SellerInsights(
    String peakPerformanceDay,
    String peakPerformanceHour,
    Double averageTransactionValue,
    Double customerRetentionRate,
    Double repeatCustomerRate,
    Double newCustomerRate,
    Double conversionRate,
    Double satisfactionScore
) {
    public static SellerInsights empty() {
        return new SellerInsights("", "", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }
}