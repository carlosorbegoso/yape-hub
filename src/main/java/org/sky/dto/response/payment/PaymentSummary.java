package org.sky.dto.response.payment;

public record PaymentSummary(
    Long totalPayments,
    Long pendingCount,
    Long confirmedCount,
    Long rejectedCount,
    Double totalAmount,
    Double confirmedAmount,
    Double pendingAmount
) {}
