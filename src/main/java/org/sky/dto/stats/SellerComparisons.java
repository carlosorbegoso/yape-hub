package org.sky.dto.stats;

public record SellerComparisons(
    ComparisonData vsPreviousWeek,
    ComparisonData vsPreviousMonth,
    ComparisonData vsPersonalBest,
    ComparisonData vsAverage
) {}