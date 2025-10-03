package org.sky.dto.response.admin;

public record GrowthOpportunities(
    Long potentialNewBranches,
    String marketExpansion,
    Long sellerRecruitment,
    Double revenueProjection
) {}