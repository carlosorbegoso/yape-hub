package org.sky.service.stats.calculators.strategy;

/**
 * Record para encapsular la entrada de cálculo de comisiones
 */
public record CommissionInput(
    Double amount,
    Double commissionRate
) {}
