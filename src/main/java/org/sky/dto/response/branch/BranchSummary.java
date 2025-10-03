package org.sky.dto.response.branch;

public record BranchSummary(
    String branchName,
    Double totalSales,
    Long totalTransactions,
    Double performanceScore
) {
    public static BranchSummary empty() {
        return new BranchSummary("", 0.0, 0L, 0.0);
    }
}