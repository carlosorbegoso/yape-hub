package org.sky.dto.stats;

public record BranchSummary(
    String branchName,
    Double totalSales,
    Long totalTransactions,
    Double performanceScore
) {}