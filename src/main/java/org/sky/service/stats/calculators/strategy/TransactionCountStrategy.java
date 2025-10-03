package org.sky.service.stats.calculators.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentNotificationEntity;
import java.util.List;

/**
 * Estrategia para contar el número total de transacciones
 */
@ApplicationScoped
public class TransactionCountStrategy implements StatsCalculationStrategy<List<PaymentNotificationEntity>, Long> {
    
    @Override
    public Long calculate(List<PaymentNotificationEntity> payments) {
        if (payments == null) {
            return 0L;
        }
        return (long) payments.size();
    }
    
    @Override
    public String getCalculationType() {
        return "TRANSACTION_COUNT";
    }
    
    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments) {
        return payments != null;
    }
}
