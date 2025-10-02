package org.sky.dto.stats.financial;

public record RevenueByBranch(
    Long branchId,
    String branchName,
    Double totalRevenue,
    Long totalTransactions,
    Double averageTransactionValue
) {}
