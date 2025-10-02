package org.sky.dto.stats;

public record PerformanceMetrics(
    Double averageConfirmationTime,
    Double claimRate,
    Double rejectionRate,
    Long pendingPayments,
    Long confirmedPayments,
    Long rejectedPayments
) {}