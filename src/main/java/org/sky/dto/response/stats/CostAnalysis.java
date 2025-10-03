package org.sky.dto.response.stats;

public record CostAnalysis(
    Double operationalCosts,
    Double sellerCommissions,
    Double systemMaintenance,
    Double netProfit,
    Double profitMargin
) {}