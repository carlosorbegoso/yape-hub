package org.sky.dto.response.stats;

public record RevenueByBranch(
    Long branchId,
    String branchName,
    Double revenue,
    Double percentage
) {}