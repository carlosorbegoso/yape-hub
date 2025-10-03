package org.sky.dto.response.stats;

public record CostAnalysis(
    Double operationalCosts,
    Double sellerCommissions,
    Double systemMaintenance,
    Double netProfit,
    Double profitMargin
) {
    public static CostAnalysis empty() {
        return new CostAnalysis(0.0, 0.0, 0.0, 0.0, 0.0);
    }
}