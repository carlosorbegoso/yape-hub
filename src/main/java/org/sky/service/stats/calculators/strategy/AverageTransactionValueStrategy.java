package org.sky.service.stats.calculators.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentNotificationEntity;
import java.util.List;

/**
 * Estrategia para calcular el valor promedio de las transacciones
 */
@ApplicationScoped
public class AverageTransactionValueStrategy implements StatsCalculationStrategy<List<PaymentNotificationEntity>, Double> {
    
    @Override
    public Double calculate(List<PaymentNotificationEntity> payments) {
        if (payments == null || payments.isEmpty()) {
            return 0.0;
        }
        
        return payments.stream()
                .mapToDouble(payment -> payment.amount)
                .average()
                .orElse(0.0);
    }
    
    @Override
    public String getCalculationType() {
        return "AVERAGE_TRANSACTION_VALUE";
    }
    
    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments) {
        return payments != null;
    }
}
