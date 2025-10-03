package org.sky.service.stats.calculators.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentNotificationEntity;
import java.util.List;

/**
 * Estrategia para calcular la tasa de reclamación (claim rate)
 */
@ApplicationScoped
public class ClaimRateCalculationStrategy implements StatsCalculationStrategy<List<PaymentNotificationEntity>, Double> {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    
    @Override
    public Double calculate(List<PaymentNotificationEntity> payments) {
        if (payments == null || payments.isEmpty()) {
            return 0.0;
        }
        
        long confirmedCount = payments.stream()
                .filter(payment -> CONFIRMED_STATUS.equals(payment.status))
                .count();
        
        return payments.size() > 0 ? (double) confirmedCount / payments.size() * 100 : 0.0;
    }
    
    @Override
    public String getCalculationType() {
        return "CLAIM_RATE";
    }
    
    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments) {
        return payments != null;
    }
}
