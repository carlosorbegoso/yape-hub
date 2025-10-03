package org.sky.dto.response.admin;

import java.util.List;

public record AdministrativeInsights(
    List<ManagementAlert> managementAlerts,
    List<String> recommendations,
    GrowthOpportunities growthOpportunities
) {
    public static AdministrativeInsights empty() {
        return new AdministrativeInsights(List.of(), List.of(), GrowthOpportunities.empty());
    }
}
