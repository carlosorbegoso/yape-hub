package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Strategy Pattern: Calcula métricas de rendimiento de vendedores
 * Clean Code: Responsabilidad única - solo calcula rendimiento de vendedores
 */
@ApplicationScoped
public class SellerPerformanceStrategy implements CalculationStrategy<Map<String, Object>> {

    private static final Logger log = Logger.getLogger(SellerPerformanceStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;

    @Override
    public Uni<Map<String, Object>> calculate(List<PaymentNotificationEntity> payments,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 SellerPerformanceStrategy: Calculando rendimiento para " + payments.size() + " pagos");

            // Si no hay pagos, retornar performance por defecto
            if (payments == null || payments.isEmpty()) {
                log.debug("⚠️ SellerPerformanceStrategy: No hay pagos, usando performance por defecto");
                return Map.<String, Object>of(
                    "bestDay", "",
                    "worstDay", "",
                    "averageDailySales", 0.0,
                    "consistencyScore", 0.0,
                    "peakPerformanceHours", List.of(),
                    "productivityScore", 0.0,
                    "efficiencyRate", 0.0,
                    "responseTime", 0.0
                );
            }

            var stream = payments.size() > PARALLEL_THRESHOLD ? 
                payments.parallelStream() : payments.stream();

            // Calcular métricas de rendimiento
            Map<LocalDate, Double> dailySales = new ConcurrentHashMap<>();
            Map<Integer, Double> hourlySales = new ConcurrentHashMap<>();
            
            stream.filter(p -> p.amount != null && p.amount > 0)
                  .forEach(payment -> {
                      LocalDate date = payment.createdAt.toLocalDate();
                      int hour = payment.createdAt.getHour();
                      double amount = payment.amount;
                      
                      dailySales.merge(date, amount, Double::sum);
                      hourlySales.merge(hour, amount, Double::sum);
                  });

            // Encontrar mejor y peor día
            String bestDay = findBestDay(dailySales);
            String worstDay = findWorstDay(dailySales);
            
            // Calcular promedio de ventas diarias
            double averageDailySales = dailySales.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

            // Calcular puntuación de consistencia
            double consistencyScore = calculateConsistencyScore(dailySales.values());

            // Encontrar horas de mayor rendimiento
            List<String> peakPerformanceHours = findPeakPerformanceHours(hourlySales);

            // Calcular puntuaciones de productividad y eficiencia
            double productivityScore = calculateProductivityScore(payments, startDate, endDate);
            double efficiencyRate = calculateEfficiencyRate(payments);
            double responseTime = calculateAverageResponseTime(payments);

            log.debug("✅ SellerPerformanceStrategy: Rendimiento calculado - Mejor día: " + bestDay);

            return Map.<String, Object>of(
                "bestDay", bestDay,
                "worstDay", worstDay,
                "averageDailySales", averageDailySales,
                "consistencyScore", consistencyScore,
                "peakPerformanceHours", peakPerformanceHours,
                "productivityScore", productivityScore,
                "efficiencyRate", efficiencyRate,
                "responseTime", responseTime
            );
        });
    }

    private String findBestDay(Map<LocalDate, Double> dailySales) {
        return dailySales.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(entry -> entry.getKey().toString())
            .orElse("");
    }

    private String findWorstDay(Map<LocalDate, Double> dailySales) {
        return dailySales.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(entry -> entry.getKey().toString())
            .orElse("");
    }

    private double calculateConsistencyScore(Collection<Double> dailySales) {
        if (dailySales.isEmpty()) return 0.0;
        
        double mean = dailySales.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (mean == 0.0) return 0.0;
        
        double variance = dailySales.stream()
            .mapToDouble(sales -> Math.pow(sales - mean, 2))
            .average()
            .orElse(0.0);
            
        double standardDeviation = Math.sqrt(variance);
        double coefficientOfVariation = standardDeviation / mean;
        
        // Puntuación de consistencia (inversa del coeficiente de variación)
        return Math.max(0.0, 100.0 - (coefficientOfVariation * 100));
    }

    private List<String> findPeakPerformanceHours(Map<Integer, Double> hourlySales) {
        return hourlySales.entrySet().stream()
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .limit(3)
            .map(entry -> String.valueOf(entry.getKey()))
            .collect(Collectors.toList());
    }

    private double calculateProductivityScore(List<PaymentNotificationEntity> payments,
                                            LocalDate startDate, LocalDate endDate) {
        if (payments.isEmpty()) return 0.0;
        
        long totalTransactions = payments.size();
        long daysBetween = startDate != null && endDate != null ? 
            java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1 : 1;
            
        double transactionsPerDay = (double) totalTransactions / daysBetween;
        
        // Puntuación basada en transacciones por día (escala 0-100)
        return Math.min(100.0, transactionsPerDay * 10);
    }

    private double calculateEfficiencyRate(List<PaymentNotificationEntity> payments) {
        if (payments.isEmpty()) return 0.0;
        
        long confirmedPayments = payments.stream()
            .filter(p -> "CONFIRMED".equals(p.status))
            .count();
            
        return (double) confirmedPayments / payments.size() * 100;
    }

    private double calculateAverageResponseTime(List<PaymentNotificationEntity> payments) {
        return payments.stream()
            .filter(p -> p.createdAt != null && p.updatedAt != null)
            .filter(p -> "CONFIRMED".equals(p.status))
            .mapToLong(p -> java.time.Duration.between(p.createdAt, p.updatedAt).toMinutes())
            .average()
            .orElse(0.0);
    }

    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments, 
                           LocalDate startDate, 
                           LocalDate endDate, 
                           Long adminId) {
        return payments != null; // Permitir listas vacías para calcular performance por defecto
    }

    @Override
    public String getStrategyName() {
        return "SellerPerformanceStrategy";
    }
}
