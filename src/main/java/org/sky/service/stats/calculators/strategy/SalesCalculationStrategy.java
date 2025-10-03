package org.sky.service.stats.calculators.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentNotificationEntity;
import java.util.List;

/**
 * Estrategia para calcular total de ventas de pagos confirmados
 */
@ApplicationScoped
public class SalesCalculationStrategy implements StatsCalculationStrategy<List<PaymentNotificationEntity>, Double> {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    
    @Override
    public Double calculate(List<PaymentNotificationEntity> payments) {
        if (payments == null || payments.isEmpty()) {
            return 0.0;
        }
        
        return payments.stream()
                .filter(payment -> CONFIRMED_STATUS.equals(payment.status))
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    @Override
    public String getCalculationType() {
        return "TOTAL_SALES";
    }
    
    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments) {
        return payments != null;
    }
}
