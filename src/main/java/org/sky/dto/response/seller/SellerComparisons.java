package org.sky.dto.response.seller;

import org.sky.dto.response.stats.ComparisonData;

public record SellerComparisons(
    ComparisonData vsPreviousWeek,
    ComparisonData vsPreviousMonth,
    ComparisonData vsPersonalBest,
    ComparisonData vsAverage
) {}