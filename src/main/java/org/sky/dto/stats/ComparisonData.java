package org.sky.dto.stats;

public record ComparisonData(
    Double salesChange,
    Long transactionChange,
    Double percentageChange
) {}