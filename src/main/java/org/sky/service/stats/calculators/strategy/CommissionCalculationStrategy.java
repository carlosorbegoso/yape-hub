package org.sky.service.stats.calculators.strategy;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Estrategia para calcular comisiones
 */
@ApplicationScoped
public class CommissionCalculationStrategy implements StatsCalculationStrategy<CommissionInput, Double> {
    
    @Override
    public Double calculate(CommissionInput input) {
        if (input == null || input.amount() == null || input.commissionRate() == null) {
            return 0.0;
        }
        
        return input.amount() * input.commissionRate();
    }
    
    @Override
    public String getCalculationType() {
        return "COMMISSION_CALCULATION";
    }
    
    @Override
    public boolean canHandle(CommissionInput input) {
        return input != null && input.amount() != null && input.commissionRate() != null;
    }
    
}
