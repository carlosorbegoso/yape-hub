package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.model.PaymentNotificationEntity;
import org.sky.service.stats.calculators.StatisticsCalculator.PerformanceMetrics;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Strategy Pattern: Implementación específica para métricas de rendimiento
 * Clean Code: Responsabilidad única - solo calcula métricas de rendimiento
 */
@ApplicationScoped
public class PerformanceMetricsStrategy implements CalculationStrategy<PerformanceMetrics> {
    
    private static final Logger log = Logger.getLogger(PerformanceMetricsStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String REJECTED_STATUS = "REJECTED";
    private static final String PENDING_STATUS = "PENDING";
    
    @Override
    public Uni<PerformanceMetrics> calculate(List<PaymentNotificationEntity> payments, 
                                           LocalDate startDate, 
                                           LocalDate endDate, 
                                           Long adminId) {
        return Uni.combine()
            .all()
            .unis(
                calculateConfirmedCount(payments),
                calculateRejectedCount(payments),
                calculatePendingCount(payments),
                calculateAverageConfirmationTime(payments)
            )
            .with((confirmedCount, rejectedCount, pendingCount, avgConfirmationTime) -> 
                buildPerformanceMetrics(confirmedCount, rejectedCount, pendingCount, avgConfirmationTime));
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
        return "PerformanceMetricsStrategy";
    }
    
    // Métodos privados para cálculos específicos
    private Uni<Long> calculateConfirmedCount(List<PaymentNotificationEntity> payments) {
        return Uni.createFrom().item(() -> 
            getStream(payments).filter(p -> CONFIRMED_STATUS.equals(p.status)).count()
        );
    }
    
    private Uni<Long> calculateRejectedCount(List<PaymentNotificationEntity> payments) {
        return Uni.createFrom().item(() -> 
            getStream(payments).filter(p -> REJECTED_STATUS.equals(p.status)).count()
        );
    }
    
    private Uni<Long> calculatePendingCount(List<PaymentNotificationEntity> payments) {
        return Uni.createFrom().item(() -> 
            getStream(payments).filter(p -> PENDING_STATUS.equals(p.status)).count()
        );
    }
    
    private Uni<Double> calculateAverageConfirmationTime(List<PaymentNotificationEntity> payments) {
        return Uni.createFrom().item(() -> 
            getStream(payments)
                .filter(this::isConfirmedWithUpdateTime)
                .mapToLong(this::calculateConfirmationTimeInMinutes)
                .average()
                .orElse(0.0)
        );
    }
    
    private PerformanceMetrics buildPerformanceMetrics(Object confirmedCount, 
                                                      Object rejectedCount, 
                                                      Object pendingCount, 
                                                      Object avgConfirmationTime) {
        // Convert Object parameters to proper types
        Long confirmed = (Long) confirmedCount;
        Long rejected = (Long) rejectedCount;
        Long pending = (Long) pendingCount;
        Double avgTime = (Double) avgConfirmationTime;
        
        log.debug("🔄 PerformanceMetricsStrategy: Confirmados=" + confirmed + ", Rechazados=" + rejected + ", Pendientes=" + pending);
        
        double claimRate = calculateClaimRate(confirmed, confirmed + rejected + pending);
        double rejectionRate = calculateRejectionRate(rejected, confirmed + rejected + pending);
        
        return new PerformanceMetrics(
            avgTime,
            claimRate,
            rejectionRate,
            pending.intValue(),
            confirmed.intValue(),
            rejected.intValue()
        );
    }
    
    // Métodos auxiliares
    private java.util.stream.Stream<PaymentNotificationEntity> getStream(List<PaymentNotificationEntity> payments) {
        return payments.size() > PARALLEL_THRESHOLD ? payments.parallelStream() : payments.stream();
    }
    
    private boolean isConfirmedWithUpdateTime(PaymentNotificationEntity payment) {
        return CONFIRMED_STATUS.equals(payment.status) && payment.updatedAt != null;
    }
    
    private long calculateConfirmationTimeInMinutes(PaymentNotificationEntity payment) {
        return Duration.between(payment.createdAt, payment.updatedAt).toMinutes();
    }
    
    private double calculateClaimRate(Long confirmedCount, Long totalCount) {
        return totalCount > 0 ? (double) confirmedCount / totalCount : 0.0;
    }
    
    private double calculateRejectionRate(Long rejectedCount, Long totalCount) {
        return totalCount > 0 ? (double) rejectedCount / totalCount : 0.0;
    }
}
