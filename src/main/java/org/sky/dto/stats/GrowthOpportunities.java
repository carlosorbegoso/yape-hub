package org.sky.dto.stats;

public record GrowthOpportunities(
    Long potentialNewBranches,
    String marketExpansion,
    Long sellerRecruitment,
    Double revenueProjection
) {}