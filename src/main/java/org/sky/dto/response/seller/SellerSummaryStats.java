package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerSummaryStats(
    Double totalSales,
    Long totalTransactions,
    Double averageTransactionValue,
    Long pendingPayments,
    Long confirmedPayments,
    Long rejectedPayments,
    Double claimRate
) {}
