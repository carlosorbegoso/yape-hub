package org.sky.dto.response.stats;

public record ComparisonData(
    Double salesChange,
    Long transactionChange,
    Double percentageChange
) {}