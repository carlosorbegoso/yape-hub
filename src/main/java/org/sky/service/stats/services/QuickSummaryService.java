package org.sky.service.stats.services;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.stats.QuickSummaryResponse;
import org.sky.model.PaymentNotification;
import org.sky.repository.PaymentNotificationRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio especializado para resúmenes rápidos
 * Responsabilidad única: generar dashboard básico
 */
@ApplicationScoped
public class QuickSummaryService {

    private static final Logger log = Logger.getLogger(QuickSummaryService.class);

    @Inject
    PaymentNotificationRepository paymentNotificationRepository;

    /**
     * Obtiene resumen rápido optimizado para dashboard
     */
    @WithTransaction
    public Uni<QuickSummaryResponse> getQuickSummary(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 QuickSummaryService.getQuickSummary() - AdminId: " + adminId);

        return paymentNotificationRepository.findPaymentsForQuickSummary(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .chain(payments -> Uni.createFrom().item(calculateQuickSummary(payments)));
    }

    /**
     * Calcula métricas básicas para dashboard
     */
    private QuickSummaryResponse calculateQuickSummary(List<PaymentNotification> payments) {
        // Métricas básicas
        double totalSales = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .mapToDouble(p -> p.amount)
                .sum();
        
        long totalTransactions = payments.size();
        double averageTransactionValue = totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
        
        // Estado de pagos
        long pendingPayments = payments.stream().filter(p -> "PENDING".equals(p.status)).count();
        long confirmedPayments = payments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
        long rejectedPayments = payments.stream().filter(p -> "REJECTED_BY_SELLER".equals(p.status)).count();
        
        // Métricas calculadas
        double claimRate = totalTransactions > 0 ? (double) confirmedPayments / totalTransactions * 100 : 0.0;
        double averageConfirmationTime = calculateAvgConfirmationTime(payments);
        
        // Sin crecimiento simulado - datos reales únicamente
        return new QuickSummaryResponse(
            totalSales, totalTransactions, averageTransactionValue,
            0.0, 0.0, 0.0,  // Crecimiento = 0 hasta tener historial
            pendingPayments, confirmedPayments, rejectedPayments,
            averageConfirmationTime, claimRate
        );
    }

    /**
     * Calcula tiempo promedio real de confirmación
     */
    private double calculateAvgConfirmationTime(List<PaymentNotification> payments) {
        if (payments.isEmpty()) return 0.0;
        
        long totalMinutes = payments.stream()
                .filter(p -> p.confirmedAt != null && p.createdAt != null)
                .mapToLong(p -> java.time.Duration.between(p.createdAt, p.confirmedAt).toMinutes())
                .sum();
        
        long confirmedCount = payments.stream()
                .filter(p -> p.confirmedAt != null)
                .count();
        
        return confirmedCount > 0 ? (double) totalMinutes / confirmedCount : 0.0;
    }
}
