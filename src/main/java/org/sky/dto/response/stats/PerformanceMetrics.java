package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PerformanceMetrics(
    Double averageConfirmationTime,
    Double claimRate,
    Double rejectionRate,
    Long pendingPayments,
    Long confirmedPayments,
    Long rejectedPayments
) {
    public static PerformanceMetrics empty() {
        return new PerformanceMetrics(0.0, 0.0, 0.0, 0L, 0L, 0L);
    }
}
