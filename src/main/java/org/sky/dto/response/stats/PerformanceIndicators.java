package org.sky.dto.response.stats;

public record PerformanceIndicators(
    Double salesVelocity,
    Double transactionVelocity,
    Double efficiencyIndex,
    Double consistencyIndex
) {}