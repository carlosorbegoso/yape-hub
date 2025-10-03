package org.sky.dto.response;

public record QuickSummarySpecificMetrics(
    Double averageConfirmationTime,
    Long pendingCount,
    Long confirmedCount,
    Long rejectedCount
) {}