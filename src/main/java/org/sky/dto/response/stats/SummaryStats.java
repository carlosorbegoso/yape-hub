package org.sky.dto.response.stats;

public record SummaryStats(
    Double totalSales,
    Long totalTransactions,
    Double averageTransactionValue,
    Long pendingPayments,
    Long confirmedPayments,
    Long rejectedPayments
) {}
