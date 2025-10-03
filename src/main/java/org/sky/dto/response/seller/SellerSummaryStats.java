package org.sky.dto.response.seller;

public record SellerSummaryStats(
    Double totalSales,
    Long totalTransactions,
    Double averageTransactionValue,
    Long pendingPayments,
    Long confirmedPayments,
    Long rejectedPayments,
    Double claimRate
) {}
