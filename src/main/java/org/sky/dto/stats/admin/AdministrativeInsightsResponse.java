package org.sky.dto.stats.admin;

import java.util.List;

public record AdministrativeInsightsResponse(
    List<ManagementAlert> managementAlerts,
    List<String> recommendations,
    GrowthOpportunities growthOpportunities
) {
    
    public record ManagementAlert(
        String type,
        String severity,
        String message,
        String affectedBranch,
        List<String> affectedSellers,
        String recommendation
    ) {}
    
    public record GrowthOpportunities(
        Long potentialNewBranches,
        String marketExpansion,
        Long sellerRecruitment,
        Double revenueProjection
    ) {}
}
