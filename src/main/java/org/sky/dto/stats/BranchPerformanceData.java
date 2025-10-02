package org.sky.dto.stats;

import java.time.LocalDateTime;

public record BranchPerformanceData(
    Long branchId,
    String branchName,
    String branchLocation,
    Double totalSales,
    Long totalTransactions,
    Long activeSellers,
    Long inactiveSellers,
    Double averageSalesPerSeller,
    Double performanceScore,
    LocalDateTime lastActivity
) {}