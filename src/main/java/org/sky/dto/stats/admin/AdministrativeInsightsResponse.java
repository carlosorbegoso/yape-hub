package org.sky.dto.stats.admin;

import org.sky.dto.stats.GrowthOpportunities;
import org.sky.dto.stats.ManagementAlert;

import java.util.List;

public record AdministrativeInsightsResponse(
    List<ManagementAlert> managementAlerts,
    List<String> recommendations,
    GrowthOpportunities growthOpportunities
) {
    

}
