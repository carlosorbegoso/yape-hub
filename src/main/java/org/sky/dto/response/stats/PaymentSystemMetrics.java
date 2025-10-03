package org.sky.dto.response.stats;
public record PaymentSystemMetrics(
    Long totalPaymentsProcessed,
    Long pendingPayments,
    Long confirmedPayments,
    Long rejectedPayments,
    Double averageConfirmationTime,
    Double paymentSuccessRate
) {
    public static PaymentSystemMetrics empty() {
        return new PaymentSystemMetrics(0L, 0L, 0L, 0L, 0.0, 0.0);
    }
}