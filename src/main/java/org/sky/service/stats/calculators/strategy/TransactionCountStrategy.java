package org.sky.service.stats.calculators.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentNotification;
import java.util.List;

/**
 * Estrategia para contar el número total de transacciones
 */
@ApplicationScoped
public class TransactionCountStrategy implements StatsCalculationStrategy<List<PaymentNotification>, Long> {
    
    @Override
    public Long calculate(List<PaymentNotification> payments) {
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
    public boolean canHandle(List<PaymentNotification> payments) {
        return payments != null;
    }
}
