package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.model.PaymentNotificationEntity;
import org.sky.service.stats.calculators.StatisticsCalculator.BasicStats;

import java.time.LocalDate;
import java.util.List;

/**
 * Strategy Pattern: Implementación específica para cálculos de estadísticas básicas
 * Clean Code: Responsabilidad única - solo calcula estadísticas básicas
 */
@ApplicationScoped
public class BasicStatsStrategy implements CalculationStrategy<BasicStats> {
    
    private static final Logger log = Logger.getLogger(BasicStatsStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;
    
    @Override
    public Uni<BasicStats> calculate(List<PaymentNotificationEntity> payments, 
                                   LocalDate startDate, 
                                   LocalDate endDate, 
                                   Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 BasicStatsStrategy: Calculando estadísticas básicas para " + payments.size() + " pagos");
            
            var stream = shouldUseParallelStream(payments.size()) ? 
                payments.parallelStream() : payments.stream();
            
            double totalSales = calculateTotalSales(stream);
            long totalTransactions = payments.size();
            double averageTransactionValue = calculateAverageTransactionValue(totalSales, totalTransactions);
            
            log.debug("✅ BasicStatsStrategy: Ventas=" + totalSales + ", Transacciones=" + totalTransactions);
            return new BasicStats(totalSales, totalTransactions, averageTransactionValue);
        });
    }
    
    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments, 
                           LocalDate startDate, 
                           LocalDate endDate, 
                           Long adminId) {
        return payments != null && !payments.isEmpty();
    }
    
    @Override
    public String getStrategyName() {
        return "BasicStatsStrategy";
    }
    
    // Métodos privados para mejorar legibilidad y reutilización
    private boolean shouldUseParallelStream(int size) {
        return size > PARALLEL_THRESHOLD;
    }
    
    private double calculateTotalSales(java.util.stream.Stream<PaymentNotificationEntity> stream) {
        return stream
            .filter(this::isValidPayment)
            .mapToDouble(payment -> payment.amount)
            .sum();
    }
    
    private boolean isValidPayment(PaymentNotificationEntity payment) {
        return payment.amount != null && payment.amount > 0;
    }
    
    private double calculateAverageTransactionValue(double totalSales, long totalTransactions) {
        return totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
    }
}
