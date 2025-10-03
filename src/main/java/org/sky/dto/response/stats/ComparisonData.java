package org.sky.dto.response.stats;

public record ComparisonData(
    Double salesChange,
    Long transactionChange,
    Double percentageChange
) {
    public static ComparisonData empty() {
        return new ComparisonData(0.0, 0L, 0.0);
    }
}