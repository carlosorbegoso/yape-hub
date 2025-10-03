package org.sky.dto.response.admin;

import java.util.List;

public record AdministrativeInsights(
    List<ManagementAlert> managementAlerts,
    List<String> recommendations,
    GrowthOpportunities growthOpportunities
) {}
