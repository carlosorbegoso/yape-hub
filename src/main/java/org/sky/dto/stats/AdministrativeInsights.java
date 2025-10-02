package org.sky.dto.stats;

import java.util.List;

public record AdministrativeInsights(
    List<ManagementAlert> managementAlerts,
    List<String> recommendations,
    GrowthOpportunities growthOpportunities
) {}
