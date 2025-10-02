package org.sky.dto.stats.financial;

public record CostAnalysis(
    Double totalCosts,
    Double operationalCosts,
    Double transactionCosts,
    Double profitMargin
) {}