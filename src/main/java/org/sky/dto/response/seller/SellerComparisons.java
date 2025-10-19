package org.sky.dto.response.seller;

import org.sky.dto.response.stats.ComparisonData;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
