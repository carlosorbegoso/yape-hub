package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenStatusResponse(
    @JsonProperty("tokensAvailable")
    Integer tokensAvailable,
    
    @JsonProperty("tokensUsed")
    Integer tokensUsed,
    
    @JsonProperty("tokensPurchased")
    Integer tokensPurchased,
    
    @JsonProperty("daysUntilReset")
    Integer daysUntilReset,
    
    @JsonProperty("usagePercentage")
    Double usagePercentage
) {}
