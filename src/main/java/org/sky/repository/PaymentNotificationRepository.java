package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentNotificationEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PaymentNotificationRepository implements PanacheRepository<PaymentNotificationEntity> {
    
    /**
     * Find pending payments for seller with pagination
     */
    public Uni<List<PaymentNotificationEntity>> findPendingPaymentsForSeller(Long sellerId, int page, int size, LocalDate startDate, LocalDate endDate) {
        return find(
            "seller.id = ?1 AND status = 'PENDING' AND createdAt BETWEEN ?2 AND ?3 ORDER BY createdAt DESC",
            sellerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        ).page(page, size).list();
    }

    /**
     * Find all pending payments with pagination
     */
    public Uni<List<PaymentNotificationEntity>> findAllPendingPayments(int page, int size, LocalDate startDate, LocalDate endDate) {
        return find(
            "status = 'PENDING' AND createdAt BETWEEN ?1 AND ?2 ORDER BY createdAt DESC",
            startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        ).page(page, size).list();
    }

    /**
     * Count pending payments for seller
     */
    public Uni<Long> countPendingPaymentsForSeller(Long sellerId, LocalDate startDate, LocalDate endDate) {
        return count(
            "seller.id = ?1 AND status = 'PENDING' AND createdAt BETWEEN ?2 AND ?3",
            sellerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        );
    }

    /**
     * Count all pending payments
     */
    public Uni<Long> countAllPendingPayments(LocalDate startDate, LocalDate endDate) {
        return count(
            "status = 'PENDING' AND createdAt BETWEEN ?1 AND ?2",
            startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        );
    }

    /**
     * Update payment status
     */
    public Uni<PaymentNotificationEntity> updatePaymentStatus(Long paymentId, String status) {
        return findById(paymentId)
            .chain(payment -> {
                if (payment != null) {
                    payment.status = status;
                    return persist(payment);
                }
                return Uni.createFrom().nullItem();
            });
    }
    
    // ==================================================================================
    // CONSULTAS PARA ESTADÍSTICAS (MOVIDAS DESDE StatsService)
    // ==================================================================================
    
    /**
     * Find payments by admin ID within date range for stats (OPLIMIZADO para pocos recursos)
     */
    public Uni<List<PaymentNotificationEntity>> findPaymentsForStatsByAdminId(Long adminId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3 ORDER BY createdAt DESC", adminId, startDateTime, endDateTime)
                .page(0, 5000)  // LÍMITE CRÍTICO: máximo 5000 pagos para evitar OOM
                .list();
    }
    
    /**
     * Find payments by seller ID within date range for stats
     */
    public Uni<List<PaymentNotificationEntity>> findPaymentsForStatsBySellerId(Long sellerId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return find("seller.id = ?1 and createdAt >= ?2 and createdAt <= ?3 ORDER BY createdAt DESC", sellerId, startDateTime, endDateTime)
                .page(0, 5000)  // LÍMITE CRÍTICO: máximo 5000 pagos
                .list();
    }
    
    /**
     * Find payments by confirmed by admin within date range
     */
    public Uni<List<PaymentNotificationEntity>> findPaymentsConfirmedByAdmin(Long adminId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return find("confirmedBy = ?1 and createdAt >= ?2 and createdAt <= ?3 ORDER BY createdAt DESC", adminId, startDateTime, endDateTime)
                .page(0, 5000)  // LÍMITE CRÍTICO
                .list();
    }
    
    /**
     * Count payments by admin ID within date range
     */
    public Uni<Long> countPaymentsByAdminId(Long adminId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return count("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3", adminId, startDateTime, endDateTime);
    }
    
    /**
     * Count payments by seller ID within date range
     */
    public Uni<Long> countPaymentsBySellerId(Long sellerId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return count("seller.id = ?1 and createdAt >= ?2 and createdAt <= ?3", sellerId, startDateTime, endDateTime);
    }
    
    /**
     * Find payments for quick summary (limited result set for dashboard)
     */
    public Uni<List<PaymentNotificationEntity>> findPaymentsForQuickSummary(Long adminId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3 ORDER BY createdAt DESC", adminId, startDateTime, endDateTime)
                .page(0, 1000)  // LÍMITE: máximo 1000 pagos para resumen rápido
                .list();
    }
    
    /**
     * Find payments for detailed analytics with pagination
     */
    public Uni<List<PaymentNotificationEntity>> findPaymentsForAnalytics(Long adminId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3 ORDER BY createdAt DESC", adminId, startDateTime, endDateTime)
                .page(0, 3000)  // LÍMITE: máximo 3000 pagos para análisis
                .list();
    }
    
    /**
     * Find payments confirmed by specific seller
     */
    public Uni<List<PaymentNotificationEntity>> findPaymentsConfirmedBySeller(Long sellerId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return find("confirmedBy = ?1 and createdAt >= ?2 and createdAt <= ?3 ORDER BY createdAt DESC", 
                sellerId, startDateTime, endDateTime)
                .page(0, 2000)  // Límite específico para vendedor
                .list();
    }
    
    /**
     * Find payments confirmed by seller within date range for financial analytics
     */
    public Uni<List<PaymentNotificationEntity>> findByConfirmedByAndDateRange(Long sellerId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return find("confirmedBy = ?1 and createdAt >= ?2 and createdAt <= ?3 ORDER BY createdAt DESC", 
                sellerId, startDateTime, endDateTime)
                .page(0, 2000)  // Límite para análisis financiero
                .list();
    }
    
    public Uni<List<PaymentNotificationEntity>> findByAdminIdAndDateRange(Long adminId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3 ORDER BY createdAt DESC", 
                adminId, startDateTime, endDateTime)
                .page(0, 5000)  // Límite para analytics
                .list();
    }
    
    /**
     * Count payments by status for specific admin
     */
    public Uni<Long> countPaymentsByStatus(Long adminId, String status, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return count("adminId = ?1 and status = ?2 and createdAt >= ?3 and createdAt <= ?4", 
                adminId, status, startDateTime, endDateTime);
    }
    
    /**
     * Obtiene tendencias de pagos por día para un admin específico
     * Implementación simplificada para eficiencia de recursos
     */
    public Uni<List<PaymentTrendResult>> getPaymentTrends(Long adminId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        return find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3 ORDER BY DATE(createdAt), status", 
                adminId, startDateTime, endDateTime)
                .range(0, 1000) // LIMIT para eficiencia
                .list()
                .map(payments -> {
                    // Agrupar por fecha y calcular estadísticas
                    Map<LocalDate, PaymentTrendResult> trends = new HashMap<>();
                    
                    for (PaymentNotificationEntity payment : payments) {
                        LocalDate date = payment.createdAt.toLocalDate();
                        
                        PaymentTrendResult existing = trends.get(date);
                        if (existing == null) {
                            trends.put(date, new PaymentTrendResult(date, 1, 
                                "CONFIRMED".equals(payment.status) ? payment.amount.doubleValue() : 0.0,
                                "CONFIRMED".equals(payment.status) ? 1 : 0));
                        } else {
                            trends.put(date, new PaymentTrendResult(date, 
                                existing.totalCount() + 1,
                                existing.confirmedAmount() + ("CONFIRMED".equals(payment.status) ? payment.amount.doubleValue() : 0.0),
                                existing.confirmedCount() + ("CONFIRMED".equals(payment.status) ? 1 : 0)));
                        }
                    }
                    
                    return new ArrayList<>(trends.values());
                });
    }
    
    /**
     * Result records
     */
    public record DailyStatsResult(LocalDate date, int count, double totalAmount) {}
    public record PaymentTrendResult(LocalDate date, int totalCount, double confirmedAmount, int confirmedCount) {}
}
