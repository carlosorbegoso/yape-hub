package org.sky.dto.stats;

public record QuickSummaryResponse(
    Double totalSales,
    Long totalTransactions,
    Double averageTransactionValue,
    Double salesGrowth,
    Double transactionGrowth,
    Double averageGrowth,
    Long pendingPayments,
    Long confirmedPayments,
    Long rejectedPayments,
    Double claimRate,
    Double averageConfirmationTime
) {}
