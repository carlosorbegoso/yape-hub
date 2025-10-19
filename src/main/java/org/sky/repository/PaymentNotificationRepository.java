package org.sky.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentNotificationEntity;
import org.sky.util.DeadlockRetryService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentNotificationRepository implements PanacheRepository<PaymentNotificationEntity> {
    
    @Inject
    DeadlockRetryService deadlockRetryService;
    
    /**
     * Find pending payments for seller with pagination
     */
    public Uni<List<PaymentNotificationEntity>> findPendingPaymentsForSeller(Long sellerId, int page, int size, LocalDate startDate, LocalDate endDate) {
        // Los sellers ven todos los pagos pendientes de su admin
        // Necesitamos obtener el adminId del seller primero - esto se hace en el servicio
        return find(
            "status = 'PENDING' AND createdAt BETWEEN ?1 AND ?2 ORDER BY createdAt DESC",
            startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
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
        // Los sellers ven todos los pagos pendientes de su admin
        return count(
            "status = 'PENDING' AND createdAt BETWEEN ?1 AND ?2",
            startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
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
        return deadlockRetryService.executeWithRetry(
            () -> updatePaymentStatusInternal(paymentId, status),
            "updatePaymentStatus(id=" + paymentId + ", status=" + status + ")"
        );
    }
    
    /**
     * Implementación interna del update payment status con retry automático
     */
    private Uni<PaymentNotificationEntity> updatePaymentStatusInternal(Long paymentId, String status) {
        return findById(paymentId)
            .chain(payment -> {
                if (payment != null) {
                    payment.status = status;
                    
                    // Actualizar campos específicos según el estado
                    if ("CLAIMED".equals(status)) {
                        payment.confirmedAt = java.time.LocalDateTime.now();
                        // confirmedBy se puede establecer desde el servicio si es necesario
                    } else if ("REJECTED".equals(status)) {
                        payment.rejectedAt = java.time.LocalDateTime.now();
                        // rejectedBy se puede establecer desde el servicio si es necesario
                    }
                    
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
        return find("seller = ?1 and createdAt >= ?2 and createdAt <= ?3 ORDER BY createdAt DESC", sellerId, startDateTime, endDateTime)
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
        return count("seller = ?1 and createdAt >= ?2 and createdAt <= ?3", sellerId, startDateTime, endDateTime);
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
     * Find pending payments for admin (all payments from their sellers)
     */
    public Uni<List<PaymentNotificationEntity>> findPendingPaymentsForAdmin(Long adminId, int page, int size, LocalDate startDate, LocalDate endDate) {
        return find(
            "adminId = ?1 AND status = 'PENDING' AND createdAt BETWEEN ?2 AND ?3 ORDER BY createdAt DESC",
            adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        ).page(page, size).list();
    }
    
    /**
     * Count pending payments for admin
     */
    public Uni<Long> countPendingPaymentsForAdmin(Long adminId, LocalDate startDate, LocalDate endDate) {
        return count(
            "adminId = ?1 AND status = 'PENDING' AND createdAt BETWEEN ?2 AND ?3",
            adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
        );
    }
    
    /**
     * Find payments for admin by specific status(es)
     * Supports single status or multiple comma-separated statuses (e.g., "PENDING,CLAIMED")
     */
    public Uni<List<PaymentNotificationEntity>> findPaymentsForAdminByStatus(Long adminId, int page, int size, String status, LocalDate startDate, LocalDate endDate) {
        if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) {
            // Si no se especifica estado o es "ALL", obtener todos los pagos del admin
            return find(
                "adminId = ?1 AND createdAt BETWEEN ?2 AND ?3 ORDER BY createdAt DESC",
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
            ).page(page, size).list();
        } else {
            // Procesar múltiples estados separados por comas
            String[] statuses = status.split(",");
            if (statuses.length == 1) {
                // Un solo estado
                return find(
                    "adminId = ?1 AND status = ?2 AND createdAt BETWEEN ?3 AND ?4 ORDER BY createdAt DESC",
                    adminId, statuses[0].trim(), startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
                ).page(page, size).list();
            } else {
                // Múltiples estados - usar IN clause
                StringBuilder query = new StringBuilder("adminId = ?1 AND status IN (");
                for (int i = 0; i < statuses.length; i++) {
                    if (i > 0) query.append(",");
                    query.append("?").append(i + 2);
                }
                query.append(") AND createdAt BETWEEN ?").append(statuses.length + 2).append(" AND ?").append(statuses.length + 3).append(" ORDER BY createdAt DESC");
                
                Object[] params = new Object[statuses.length + 3];
                params[0] = adminId;
                for (int i = 0; i < statuses.length; i++) {
                    params[i + 1] = statuses[i].trim();
                }
                params[statuses.length + 1] = startDate.atStartOfDay();
                params[statuses.length + 2] = endDate.atTime(23, 59, 59);
                
                return find(query.toString(), params).page(page, size).list();
            }
        }
    }
    
    /**
     * Count payments for admin by specific status(es)
     * Supports single status or multiple comma-separated statuses (e.g., "PENDING,CLAIMED")
     */
    public Uni<Long> countPaymentsForAdminByStatus(Long adminId, String status, LocalDate startDate, LocalDate endDate) {
        if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) {
            // Si no se especifica estado o es "ALL", contar todos los pagos del admin
            return count(
                "adminId = ?1 AND createdAt BETWEEN ?2 AND ?3",
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
            );
        } else {
            // Procesar múltiples estados separados por comas
            String[] statuses = status.split(",");
            if (statuses.length == 1) {
                // Un solo estado
                return count(
                    "adminId = ?1 AND status = ?2 AND createdAt BETWEEN ?3 AND ?4",
                    adminId, statuses[0].trim(), startDate.atStartOfDay(), endDate.atTime(23, 59, 59)
                );
            } else {
                // Múltiples estados - usar IN clause
                StringBuilder query = new StringBuilder("adminId = ?1 AND status IN (");
                for (int i = 0; i < statuses.length; i++) {
                    if (i > 0) query.append(",");
                    query.append("?").append(i + 2);
                }
                query.append(") AND createdAt BETWEEN ?").append(statuses.length + 2).append(" AND ?").append(statuses.length + 3);
                
                Object[] params = new Object[statuses.length + 3];
                params[0] = adminId;
                for (int i = 0; i < statuses.length; i++) {
                    params[i + 1] = statuses[i].trim();
                }
                params[statuses.length + 1] = startDate.atStartOfDay();
                params[statuses.length + 2] = endDate.atTime(23, 59, 59);
                
                return count(query.toString(), params);
            }
        }
    }

    /**
     * Sum total amount for payments by admin and status
     * Supports single status or multiple comma-separated statuses
     */
    public Uni<Double> sumAmountForAdminByStatus(Long adminId, String status, LocalDate startDate, LocalDate endDate) {
        if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) {
            // Si no se especifica estado o es "ALL", sumar todos los pagos del admin
            return find("adminId = ?1 AND createdAt BETWEEN ?2 AND ?3", 
                    adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .list()
                .map(payments -> payments.stream()
                    .mapToDouble(p -> p.amount.doubleValue())
                    .sum());
        } else {
            // Procesar múltiples estados separados por comas
            String[] statuses = status.split(",");
            if (statuses.length == 1) {
                // Un solo estado
                return find("adminId = ?1 AND status = ?2 AND createdAt BETWEEN ?3 AND ?4", 
                        adminId, statuses[0].trim(), startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                    .list()
                    .map(payments -> payments.stream()
                        .mapToDouble(p -> p.amount.doubleValue())
                        .sum());
            } else {
                // Múltiples estados - usar IN clause
                StringBuilder query = new StringBuilder("adminId = ?1 AND status IN (");
                for (int i = 0; i < statuses.length; i++) {
                    if (i > 0) query.append(",");
                    query.append("?").append(i + 2);
                }
                query.append(") AND createdAt BETWEEN ?").append(statuses.length + 2).append(" AND ?").append(statuses.length + 3);
                
                Object[] params = new Object[statuses.length + 3];
                params[0] = adminId;
                for (int i = 0; i < statuses.length; i++) {
                    params[i + 1] = statuses[i].trim();
                }
                params[statuses.length + 1] = startDate.atStartOfDay();
                params[statuses.length + 2] = endDate.atTime(23, 59, 59);
                
                return find(query.toString(), params)
                    .list()
                    .map(payments -> payments.stream()
                        .mapToDouble(p -> p.amount.doubleValue())
                        .sum());
            }
        }
    }

    
    /**
     * Result records
     */
    public record DailyStatsResult(LocalDate date, int count, double totalAmount) {}
    public record PaymentTrendResult(LocalDate date, int totalCount, double confirmedAmount, int confirmedCount) {}
}
