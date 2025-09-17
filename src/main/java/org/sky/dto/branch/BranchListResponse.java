package org.sky.dto.branch;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BranchListResponse(
    @JsonProperty("branches")
    List<BranchResponse> branches,
    
    @JsonProperty("pagination")
    PaginationInfo pagination
) {
    public record PaginationInfo(
        @JsonProperty("currentPage")
        int currentPage,
        
        @JsonProperty("totalPages")
        int totalPages,
        
        @JsonProperty("totalItems")
        long totalItems,
        
        @JsonProperty("itemsPerPage")
        int itemsPerPage
    ) {}
}
