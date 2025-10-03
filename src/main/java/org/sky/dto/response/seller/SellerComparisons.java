package org.sky.dto.response.seller;

import org.sky.dto.response.stats.ComparisonData;

public record SellerComparisons(
    ComparisonData vsPreviousWeek,
    ComparisonData vsPreviousMonth,
    ComparisonData vsPersonalBest,
    ComparisonData vsAverage
) {
    public static SellerComparisons empty() {
        return new SellerComparisons(
            ComparisonData.empty(),
            ComparisonData.empty(),
            ComparisonData.empty(),
            ComparisonData.empty()
        );
    }
}