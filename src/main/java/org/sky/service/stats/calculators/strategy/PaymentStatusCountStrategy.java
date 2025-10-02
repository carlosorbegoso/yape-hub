package org.sky.service.stats.calculators.strategy;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Estrategia para contar pagos por estado específico
 */
@ApplicationScoped
public class PaymentStatusCountStrategy implements StatsCalculationStrategy<PaymentStatusInput, Long> {
    
    @Override
    public Long calculate(PaymentStatusInput input) {
        if (input == null || input.payments() == null || input.status() == null) {
            return 0L;
        }
        
        return input.payments().stream()
                .filter(payment -> input.status().equals(payment.status))
                .count();
    }
    
    @Override
    public String getCalculationType() {
        return "PAYMENT_STATUS_COUNT";
    }
    
    @Override
    public boolean canHandle(PaymentStatusInput input) {
        return input != null && input.payments() != null && input.status() != null;
    }
}
