package org.sky.dto.response.branch;

public record BranchSummary(
    String branchName,
    Double totalSales,
    Long totalTransactions,
    Double performanceScore
) {}