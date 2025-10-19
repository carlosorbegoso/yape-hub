package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SummaryStats(
    Double totalSales,
    Long totalTransactions,
    Double averageTransactionValue,
    Long pendingPayments,
    Long confirmedPayments,
    Long rejectedPayments
) {}
