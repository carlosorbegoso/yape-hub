package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
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
