package org.sky.service.stats.calculators.strategy;

/**
 * Record para encapsular la entrada de cálculo de impuestos
 */
public record TaxInput(
    Double amount,
    Double taxRate
) {}
