package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MonthlyUsageResponse(
        @JsonProperty("tokensUsed") Long tokensUsed,
        @JsonProperty("tokensRemaining") Long tokensRemaining,
        @JsonProperty("operationsCount") Long operationsCount,
        @JsonProperty("mostUsedOperation") String mostUsedOperation
) {}
