package org.sky.dto.response.stats;

public record PerformanceMetrics(
    Double averageConfirmationTime,
    Double claimRate,
    Double rejectionRate,
    Long pendingPayments,
    Long confirmedPayments,
    Long rejectedPayments
) {}