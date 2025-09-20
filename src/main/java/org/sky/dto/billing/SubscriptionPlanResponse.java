package org.sky.dto.billing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record SubscriptionPlanResponse(
    @JsonProperty("id")
    Long id,
    
    @JsonProperty("name")
    String name,
    
    @JsonProperty("description")
    String description,
    
    @JsonProperty("pricePen")
    Double pricePen,
    
    @JsonProperty("billingCycle")
    String billingCycle,
    
    @JsonProperty("maxAdmins")
    Integer maxAdmins,
    
    @JsonProperty("maxSellers")
    Integer maxSellers,
    
    @JsonProperty("tokensIncluded")
    Integer tokensIncluded,
    
    @JsonProperty("features")
    String features,
    
    @JsonProperty("isActive")
    Boolean isActive,
    
    @JsonProperty("createdAt")
    LocalDateTime createdAt
) {}
