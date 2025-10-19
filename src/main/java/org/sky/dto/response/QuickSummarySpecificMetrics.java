package org.sky.dto.response;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record QuickSummarySpecificMetrics(
    Double averageConfirmationTime,
    Long pendingCount,
    Long confirmedCount,
    Long rejectedCount
) {}
