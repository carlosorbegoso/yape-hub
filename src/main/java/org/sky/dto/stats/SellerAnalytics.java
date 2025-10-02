package org.sky.dto.stats;

public record SellerAnalytics(
    SalesDistribution salesDistribution,
    TransactionPatterns transactionPatterns,
    PerformanceIndicators performanceIndicators
) {}