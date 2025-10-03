package org.sky.dto.response.billing;

public record MonthlyUsageResponse(
        Long tokensUsed,
        Long tokensRemaining,
        Long operationsCount,
        String mostUsedOperation
) {}
