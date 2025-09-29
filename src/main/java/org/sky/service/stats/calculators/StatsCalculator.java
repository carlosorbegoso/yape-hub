package org.sky.service.stats.calculators;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.model.PaymentNotification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculador básico para métricas estadísticas
 * Responsabilidad única: cálculos matemáticos simples
 */
@ApplicationScoped
public class StatsCalculator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Calcula estadísticas básicas de pagos
     */
    public PaymentBasicStats calculateBasicStats(List<PaymentNotification> payments) {
        double totalSales = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .mapToDouble(p -> p.amount)
                .sum();
        
        long totalTransactions = payments.size();
        long confirmedTransactions = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .count();
        
        double averageTransactionValue = confirmedTransactions > 0 ? totalSales / confirmedTransactions : 0.0;
        
        return new PaymentBasicStats(totalSales, totalTransactions, confirmedTransactions, averageTransactionValue);
    }

    /**
     * Calcula tiempo promedio real de confirmación sin simulación
     */
    public double calculateAvgConfirmationTime(List<PaymentNotification> payments) {
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

    /**
     * Agrupa pagos por fecha para análisis diario
     */
    public Map<String, List<PaymentNotification>> groupPaymentsByDate(List<PaymentNotification> payments) {
        return payments.stream()
                .collect(Collectors.groupingBy(p -> p.createdAt.toLocalDate().format(DATE_FORMATTER)));
    }

    /**
     * Record para estadísticas básicas
     */
    public record PaymentBasicStats(
            double totalSales,
            long totalTransactions,
            long confirmedTransactions,
            double averageTransactionValue
    ) {}
}
