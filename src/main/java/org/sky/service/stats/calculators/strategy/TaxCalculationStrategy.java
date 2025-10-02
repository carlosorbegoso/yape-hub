package org.sky.service.stats.calculators.strategy;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Estrategia para calcular impuestos
 */
@ApplicationScoped
public class TaxCalculationStrategy implements StatsCalculationStrategy<TaxInput, Double> {
    
    @Override
    public Double calculate(TaxInput input) {
        if (input == null || input.amount() == null || input.taxRate() == null) {
            return 0.0;
        }
        
        return input.amount() * input.taxRate();
    }
    
    @Override
    public String getCalculationType() {
        return "TAX_CALCULATION";
    }
    
    @Override
    public boolean canHandle(TaxInput input) {
        return input != null && input.amount() != null && input.taxRate() != null;
    }
    
}
