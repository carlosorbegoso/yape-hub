package org.sky.dto.response.admin;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AdministrativeInsightsResponse(
    List<ManagementAlert> managementAlerts,
    List<String> recommendations,
    GrowthOpportunities growthOpportunities
) {}
