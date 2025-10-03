package org.sky.dto.response.stats;

public record PerformanceIndicators(
    Double salesVelocity,
    Double transactionVelocity,
    Double efficiencyIndex,
    Double consistencyIndex
) {
    public static PerformanceIndicators empty() {
        return new PerformanceIndicators(0.0, 0.0, 0.0, 0.0);
    }
}