package org.sky.dto.stats;
public record PaymentSystemMetrics(
    Long totalPaymentsProcessed,
    Long pendingPayments,
    Long confirmedPayments,
    Long rejectedPayments,
    Double averageConfirmationTime,
    Double paymentSuccessRate
) {}