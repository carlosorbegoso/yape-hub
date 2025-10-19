package org.sky.dto.response.admin;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record GrowthOpportunities(
    Long potentialNewBranches,
    String marketExpansion,
    Long sellerRecruitment,
    Double revenueProjection
) {
    public static GrowthOpportunities empty() {
        return new GrowthOpportunities(0L, "", 0L, 0.0);
    }
}
