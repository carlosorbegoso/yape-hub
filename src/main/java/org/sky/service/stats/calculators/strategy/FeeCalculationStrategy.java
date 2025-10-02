package org.sky.service.stats.calculators.strategy;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Estrategia para calcular fees
 */
@ApplicationScoped
public class FeeCalculationStrategy implements StatsCalculationStrategy<FeeInput, Double> {
    
    @Override
    public Double calculate(FeeInput input) {
        if (input == null || input.amount() == null || input.feeRate() == null) {
            return 0.0;
        }
        
        return input.amount() * input.feeRate();
    }
    
    @Override
    public String getCalculationType() {
        return "FEE_CALCULATION";
    }
    
    @Override
    public boolean canHandle(FeeInput input) {
        return input != null && input.amount() != null && input.feeRate() != null;
    }
    
}
