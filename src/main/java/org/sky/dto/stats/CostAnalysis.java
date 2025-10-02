package org.sky.dto.stats;

public record CostAnalysis(
    Double operationalCosts,
    Double sellerCommissions,
    Double systemMaintenance,
    Double netProfit,
    Double profitMargin
) {}