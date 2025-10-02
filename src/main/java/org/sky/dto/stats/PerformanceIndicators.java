package org.sky.dto.stats;

public record PerformanceIndicators(
    Double salesVelocity,
    Double transactionVelocity,
    Double efficiencyIndex,
    Double consistencyIndex
) {}