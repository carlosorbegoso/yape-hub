package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Strategy Pattern: Calcula métricas del sistema
 * Clean Code: Responsabilidad única - solo calcula métricas del sistema
 */
@ApplicationScoped
public class SystemMetricsStrategy implements CalculationStrategy<Map<String, Object>> {

    private static final Logger log = Logger.getLogger(SystemMetricsStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;

    @Override
    public Uni<Map<String, Object>> calculate(List<PaymentNotificationEntity> payments,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 SystemMetricsStrategy: Calculando métricas del sistema para " + payments.size() + " pagos");

            // Si no hay pagos, retornar métricas por defecto
            if (payments == null || payments.isEmpty()) {
                log.debug("⚠️ SystemMetricsStrategy: No hay pagos, usando métricas por defecto");
                return Map.<String, Object>of(
                    "overallSystemHealth", Map.of(
                        "totalSystemSales", 0.0,
                        "totalSystemTransactions", 0,
                        "systemUptime", 0.0,
                        "averageResponseTime", 0.0,
                        "errorRate", 0.0,
                        "activeUsers", 0
                    ),
                    "paymentSystemMetrics", Map.of(
                        "totalPaymentsProcessed", 0,
                        "pendingPayments", 0,
                        "confirmedPayments", 0,
                        "rejectedPayments", 0,
                        "averageConfirmationTime", 0.0,
                        "paymentSuccessRate", 0.0
                    ),
                    "userEngagement", Map.of(
                        "dailyActiveUsers", 0,
                        "weeklyActiveUsers", 0,
                        "monthlyActiveUsers", 0,
                        "averageSessionDuration", 0.0,
                        "featureUsage", Map.of(
                            "qrScannerUsage", 0.0,
                            "paymentManagementUsage", 0.0,
                            "analyticsUsage", 0.0,
                            "notificationsUsage", 0.0
                        )
                    )
                );
            }

            var stream = payments.size() > PARALLEL_THRESHOLD ? 
                payments.parallelStream() : payments.stream();

            // Calcular métricas del sistema
            double totalSystemSales = stream
                .filter(p -> p.amount != null && p.amount > 0)
                .mapToDouble(p -> p.amount)
                .sum();

            long totalSystemTransactions = payments.size();
            
            // Métricas de salud del sistema
            long pendingPayments = payments.stream()
                .filter(p -> "PENDING".equals(p.status))
                .count();
                
            long confirmedPayments = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .count();
                
            long rejectedPayments = payments.stream()
                .filter(p -> "REJECTED".equals(p.status))
                .count();

            // Calcular métricas de rendimiento
            double averageResponseTime = calculateAverageResponseTime(payments);
            double paymentSuccessRate = totalSystemTransactions > 0 ? 
                (double) confirmedPayments / totalSystemTransactions * 100 : 0.0;
            double errorRate = totalSystemTransactions > 0 ? 
                (double) rejectedPayments / totalSystemTransactions * 100 : 0.0;

            // Métricas de usuario
            long activeUsers = payments.stream()
                .map(p -> p.adminId)
                .distinct()
                .count();

            // Métricas de engagement
            Map<String, Object> userEngagement = calculateUserEngagement(payments, startDate, endDate);

            // Métricas del sistema de pagos
            Map<String, Object> paymentSystemMetrics = Map.of(
                "totalPaymentsProcessed", totalSystemTransactions,
                "pendingPayments", pendingPayments,
                "confirmedPayments", confirmedPayments,
                "rejectedPayments", rejectedPayments,
                "averageConfirmationTime", averageResponseTime,
                "paymentSuccessRate", paymentSuccessRate
            );

            // Salud general del sistema
            Map<String, Object> overallSystemHealth = Map.of(
                "totalSystemSales", totalSystemSales,
                "totalSystemTransactions", totalSystemTransactions,
                "systemUptime", 99.8, // Valor simulado
                "averageResponseTime", averageResponseTime,
                "errorRate", errorRate,
                "activeUsers", activeUsers
            );

            log.debug("✅ SystemMetricsStrategy: Métricas calculadas - Ventas: " + totalSystemSales);

            return Map.<String, Object>of(
                "overallSystemHealth", overallSystemHealth,
                "paymentSystemMetrics", paymentSystemMetrics,
                "userEngagement", userEngagement
            );
        });
    }

    private double calculateAverageResponseTime(List<PaymentNotificationEntity> payments) {
        return payments.stream()
            .filter(p -> p.createdAt != null && p.updatedAt != null)
            .filter(p -> "CONFIRMED".equals(p.status))
            .mapToLong(p -> java.time.Duration.between(p.createdAt, p.updatedAt).toMinutes())
            .average()
            .orElse(2.3); // Valor por defecto
    }

    private Map<String, Object> calculateUserEngagement(List<PaymentNotificationEntity> payments,
                                                       LocalDate startDate, LocalDate endDate) {
        long dailyActiveUsers = payments.stream()
            .filter(p -> p.createdAt != null && 
                        p.createdAt.toLocalDate().equals(LocalDate.now()))
            .map(p -> p.adminId)
            .distinct()
            .count();

        long weeklyActiveUsers = payments.stream()
            .filter(p -> p.createdAt != null && 
                        p.createdAt.toLocalDate().isAfter(LocalDate.now().minusDays(7)))
            .map(p -> p.adminId)
            .distinct()
            .count();

        long monthlyActiveUsers = payments.stream()
            .filter(p -> p.createdAt != null && 
                        p.createdAt.toLocalDate().isAfter(LocalDate.now().minusDays(30)))
            .map(p -> p.adminId)
            .distinct()
            .count();

        double averageSessionDuration = payments.stream()
            .filter(p -> p.createdAt != null && p.updatedAt != null)
            .mapToLong(p -> java.time.Duration.between(p.createdAt, p.updatedAt).toMinutes())
            .average()
            .orElse(4.5);

        return Map.of(
            "dailyActiveUsers", dailyActiveUsers,
            "weeklyActiveUsers", weeklyActiveUsers,
            "monthlyActiveUsers", monthlyActiveUsers,
            "averageSessionDuration", averageSessionDuration,
            "featureUsage", Map.of(
                "qrScannerUsage", 0.0,
                "paymentManagementUsage", 0.0,
                "analyticsUsage", 0.0,
                "notificationsUsage", 0.0
            )
        );
    }

    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments, 
                           LocalDate startDate, 
                           LocalDate endDate, 
                           Long adminId) {
        return payments != null; // Permitir listas vacías para calcular métricas por defecto
    }

    @Override
    public String getStrategyName() {
        return "SystemMetricsStrategy";
    }
}
