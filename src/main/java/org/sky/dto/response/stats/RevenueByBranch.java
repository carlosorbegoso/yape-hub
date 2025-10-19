package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record RevenueByBranch(
    Long branchId,
    String branchName,
    Double revenue,
    Double percentage
) {}
