package org.sky.dto.branch;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record BranchResponse(
    @JsonProperty("branchId")
    Long branchId,
    
    @JsonProperty("name")
    String name,
    
    @JsonProperty("code")
    String code,
    
    @JsonProperty("address")
    String address,
    
    @JsonProperty("isActive")
    Boolean isActive,
    
    @JsonProperty("sellersCount")
    Long sellersCount,
    
    @JsonProperty("createdAt")
    LocalDateTime createdAt,
    
    @JsonProperty("updatedAt")
    LocalDateTime updatedAt
) {}
