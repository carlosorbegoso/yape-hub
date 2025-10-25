package org.sky.service.analytics;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.response.stats.*;
import org.sky.model.PaymentNotificationEntity;
import org.sky.repository.PaymentNotificationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Servicio especializado en análisis de pagos
 * Responsabilidad única: Calcular métricas relacionadas con pagos
 */
@ApplicationScoped
public class PaymentAnalyticsService {
    
    private static final Logger log = Logger.getLogger(PaymentAnalyticsService.class);
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    /**
     * Calcula métricas básicas de pagos para un admin
     */
    @WithSession
    public Uni<PaymentMetrics> calculatePaymentMetrics(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 Calculando métricas de pagos para adminId: " + adminId);
        
        return paymentNotificationRepository.findPaymentsForStatsByAdminId(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
            .map(payments -> {
                double totalSales = calculateTotalSales(payments);
                long totalTransactions = payments.size();
                double averageTransactionValue = calculateAverageTransactionValue(payments);
                long confirmedTransactions = getConfirmedTransactionsCount(payments);
                long pendingTransactions = getPendingTransactionsCount(payments);
                long rejectedTransactions = getRejectedTransactionsCount(payments);
                
                return new PaymentMetrics(
                    totalSales,
                    totalTransactions,
                    averageTransactionValue,
                    confirmedTransactions,
                    pendingTransactions,
                    rejectedTransactions
                );
            });
    }
    
    /**
     * Genera reporte de transparencia de pagos
     */
    @WithTransaction
    public Uni<Map<String, Object>> generatePaymentTransparencyReport(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 Generando reporte de transparencia para adminId: " + adminId);
        
        return paymentNotificationRepository.findPaymentsForStatsByAdminId(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
            .map(payments -> {
                double totalRevenue = calculateTotalSales(payments);
                long totalTransactions = payments.size();
                double averageTransactionValue = calculateAverageTransactionValue(payments);
                
                return Map.<String, Object>of(
                    "totalRevenue", totalRevenue,
                    "totalTransactions", totalTransactions,
                    "averageTransactionValue", averageTransactionValue,
                    "message", "Reporte de transparencia implementado - datos reales"
                );
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("❌ Error generando reporte de transparencia: " + throwable.getMessage());
                return Map.of(
                    "totalRevenue", 0.0,
                    "totalTransactions", 0,
                    "averageTransactionValue", 0.0,
                    "message", "Error generando reporte"
                );
            });
    }
    
    /**
     * Calcula métricas de crecimiento comparando con período anterior
     */
    @WithSession
    public Uni<Map<String, Double>> calculateGrowthMetrics(Long adminId, LocalDate startDate, LocalDate endDate) {
        long periodLengthDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate previousEndDate = startDate.minusDays(1);
        LocalDate previousStartDate = startDate.minusDays(periodLengthDays);
        
        return paymentNotificationRepository.findPaymentsForStatsByAdminId(
                adminId, previousStartDate.atStartOfDay(), previousEndDate.atTime(23, 59, 59))
            .chain(previousPeriodPayments -> {
                double previousSales = calculateTotalSales(previousPeriodPayments);
                long previousTransactions = previousPeriodPayments.size();
                double previousAverage = calculateAverageTransactionValue(previousPeriodPayments);
                
                // Obtener datos del período actual de forma reactiva
                return calculateCurrentPeriodSales(adminId, startDate, endDate)
                    .chain(currentSales -> 
                        calculateCurrentPeriodTransactions(adminId, startDate, endDate)
                            .chain(currentTransactions ->
                                calculateCurrentPeriodAverage(adminId, startDate, endDate)
                                    .map(currentAverage -> {
                                        // Calcular crecimiento
                                        double salesGrowth = previousSales > 0 ? 
                                            ((currentSales - previousSales) / previousSales) * 100 : 0.0;
                                        double transactionGrowth = previousTransactions > 0 ? 
                                            ((double) (currentTransactions - previousTransactions) / previousTransactions) * 100 : 0.0;
                                        double averageGrowth = previousAverage > 0 ? 
                                            ((currentAverage - previousAverage) / previousAverage) * 100 : 0.0;
                                        
                                        return Map.<String, Double>of(
                                            "salesGrowth", Math.round(salesGrowth * 10.0) / 10.0,
                                            "transactionGrowth", Math.round(transactionGrowth * 10.0) / 10.0,
                                            "averageGrowth", Math.round(averageGrowth * 10.0) / 10.0
                                        );
                                    })
                            )
                    );
            })
            .onFailure().recoverWithItem(throwable -> {
                log.warn("⚠️ Error calculating growth metrics: " + throwable.getMessage());
                return Map.of(
                    "salesGrowth", 0.0,
                    "transactionGrowth", 0.0,
                    "averageGrowth", 0.0
                );
            });
    }
    
    @WithSession
    public Uni<Double> calculateCurrentPeriodSales(Long adminId, LocalDate startDate, LocalDate endDate) {
        return paymentNotificationRepository.findPaymentsForStatsByAdminId(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
            .map(this::calculateTotalSales)
            .onFailure().recoverWithItem(throwable -> {
                log.warn("⚠️ Error calculating current period sales: " + throwable.getMessage());
                return 0.0;
            });
    }
    
    @WithTransaction
    public Uni<Long> calculateCurrentPeriodTransactions(Long adminId, LocalDate startDate, LocalDate endDate) {
        return paymentNotificationRepository.findPaymentsForStatsByAdminId(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
            .map(payments -> (long) payments.size())
            .onFailure().recoverWithItem(throwable -> {
                log.warn("⚠️ Error calculating current period transactions: " + throwable.getMessage());
                return 0L;
            });
    }
    
    @WithSession
    public Uni<Double> calculateCurrentPeriodAverage(Long adminId, LocalDate startDate, LocalDate endDate) {
        return paymentNotificationRepository.findPaymentsForStatsByAdminId(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
            .map(this::calculateAverageTransactionValue)
            .onFailure().recoverWithItem(throwable -> {
                log.warn("⚠️ Error calculating current period average: " + throwable.getMessage());
                return 0.0;
            });
    }
    
    // ==================================================================================
    // MÉTODOS AUXILIARES
    // ==================================================================================
    
    /**
     * Calcula el total de ventas de una lista de pagos
     */
    private double calculateTotalSales(List<PaymentNotificationEntity> payments) {
        return payments.stream()
            .filter(p -> "CONFIRMED".equals(p.status))
            .mapToDouble(p -> p.amount)
            .sum();
    }

    /**
     * Calcula el valor promedio de transacción
     */
    private double calculateAverageTransactionValue(List<PaymentNotificationEntity> payments) {
        long confirmedCount = getConfirmedTransactionsCount(payments);
        if (confirmedCount == 0) {
            return 0.0;
        }
        
        double totalRevenue = calculateTotalSales(payments);
        return totalRevenue / confirmedCount;
    }
    
    /**
     * Obtiene el número de transacciones confirmadas
     */
    private long getConfirmedTransactionsCount(List<PaymentNotificationEntity> payments) {
        return payments.stream()
            .filter(p -> "CONFIRMED".equals(p.status))
            .count();
    }
    
    /**
     * Obtiene el número de transacciones pendientes
     */
    private long getPendingTransactionsCount(List<PaymentNotificationEntity> payments) {
        return payments.stream()
            .filter(p -> "PENDING".equals(p.status))
            .count();
    }
    
    /**
     * Obtiene el número de transacciones rechazadas
     */
    private long getRejectedTransactionsCount(List<PaymentNotificationEntity> payments) {
        return payments.stream()
            .filter(p -> "REJECTED".equals(p.status))
            .count();
    }
    
    /**
     * Record para métricas de pagos
     */
    public record PaymentMetrics(
        double totalSales,
        long totalTransactions,
        double averageTransactionValue,
        long confirmedTransactions,
        long pendingTransactions,
        long rejectedTransactions
    ) {}
}