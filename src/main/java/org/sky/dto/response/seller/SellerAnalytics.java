package org.sky.dto.response.seller;

import org.sky.dto.response.stats.PerformanceIndicators;
import org.sky.dto.response.stats.SalesDistribution;
import org.sky.dto.response.stats.TransactionPatterns;

public record SellerAnalytics(
    SalesDistribution salesDistribution,
    TransactionPatterns transactionPatterns,
    PerformanceIndicators performanceIndicators
) {
    public static SellerAnalytics empty() {
        return new SellerAnalytics(
            SalesDistribution.empty(),
            TransactionPatterns.empty(),
            PerformanceIndicators.empty()
        );
    }
}