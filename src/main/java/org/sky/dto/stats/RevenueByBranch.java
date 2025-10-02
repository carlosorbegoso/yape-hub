package org.sky.dto.stats;

public record RevenueByBranch(
    Long branchId,
    String branchName,
    Double revenue,
    Double percentage
) {}