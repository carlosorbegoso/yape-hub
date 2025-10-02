package org.sky.service.stats.calculators.strategy;

/**
 * Record para encapsular la entrada de cálculo de fees
 */
public record FeeInput(
    Double amount,
    Double feeRate
) {}
