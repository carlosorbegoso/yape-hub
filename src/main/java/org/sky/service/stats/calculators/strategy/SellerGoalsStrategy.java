package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Strategy Pattern: Calcula objetivos y metas de vendedores
 * Clean Code: Responsabilidad única - solo calcula objetivos de vendedores
 */
@ApplicationScoped
public class SellerGoalsStrategy implements CalculationStrategy<Map<String, Object>> {

    private static final Logger log = Logger.getLogger(SellerGoalsStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;

    @Override
    public Uni<Map<String, Object>> calculate(List<PaymentNotificationEntity> payments,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 SellerGoalsStrategy: Calculando objetivos para " + payments.size() + " pagos");

            // Si no hay pagos, retornar objetivos por defecto
            if (payments == null || payments.isEmpty()) {
                log.debug("⚠️ SellerGoalsStrategy: No hay pagos, usando objetivos por defecto");
                return Map.<String, Object>of(
                    "dailyTarget", 100.0,
                    "weeklyTarget", 700.0,
                    "monthlyTarget", 3000.0,
                    "yearlyTarget", 36500.0,
                    "achievementRate", 0.0,
                    "dailyProgress", 0.0,
                    "weeklyProgress", 0.0,
                    "monthlyProgress", 0.0
                );
            }

            var stream = payments.size() > PARALLEL_THRESHOLD ? 
                payments.parallelStream() : payments.stream();

            // Calcular objetivos basados en datos históricos
            double totalSales = stream
                .filter(p -> p.amount != null && p.amount > 0)
                .mapToDouble(p -> p.amount)
                .sum();

            long totalTransactions = payments.size();
            
            // Objetivos dinámicos basados en rendimiento actual
            double dailyTarget = calculateDailyTarget(totalSales, startDate, endDate);
            double weeklyTarget = dailyTarget * 7;
            double monthlyTarget = dailyTarget * 30;
            double yearlyTarget = dailyTarget * 365;

            // Calcular progreso
            double currentDailySales = calculateCurrentDailySales(payments, LocalDate.now());
            double dailyProgress = dailyTarget > 0 ? (currentDailySales / dailyTarget) * 100 : 0.0;
            double weeklyProgress = calculateWeeklyProgress(payments, startDate, endDate, weeklyTarget);
            double monthlyProgress = calculateMonthlyProgress(payments, startDate, endDate, monthlyTarget);

            double achievementRate = calculateAchievementRate(payments, startDate, endDate);

            log.debug("✅ SellerGoalsStrategy: Objetivos calculados - Diario: " + dailyTarget);

            return Map.<String, Object>of(
                "dailyTarget", dailyTarget,
                "weeklyTarget", weeklyTarget,
                "monthlyTarget", monthlyTarget,
                "yearlyTarget", yearlyTarget,
                "achievementRate", achievementRate,
                "dailyProgress", Math.min(dailyProgress, 100.0),
                "weeklyProgress", Math.min(weeklyProgress, 100.0),
                "monthlyProgress", Math.min(monthlyProgress, 100.0)
            );
        });
    }

    private double calculateDailyTarget(double totalSales, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return 100.0;
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return daysBetween > 0 ? totalSales / daysBetween : 100.0;
    }

    private double calculateCurrentDailySales(List<PaymentNotificationEntity> payments, LocalDate today) {
        return payments.stream()
            .filter(p -> p.createdAt != null && p.createdAt.toLocalDate().equals(today))
            .filter(p -> p.amount != null && p.amount > 0)
            .mapToDouble(p -> p.amount)
            .sum();
    }

    private double calculateWeeklyProgress(List<PaymentNotificationEntity> payments, 
                                         LocalDate startDate, LocalDate endDate, 
                                         double weeklyTarget) {
        if (weeklyTarget <= 0) return 0.0;
        
        double weeklySales = payments.stream()
            .filter(p -> p.createdAt != null && 
                        !p.createdAt.toLocalDate().isBefore(startDate) && 
                        !p.createdAt.toLocalDate().isAfter(endDate))
            .filter(p -> p.amount != null && p.amount > 0)
            .mapToDouble(p -> p.amount)
            .sum();
            
        return (weeklySales / weeklyTarget) * 100;
    }

    private double calculateMonthlyProgress(List<PaymentNotificationEntity> payments,
                                          LocalDate startDate, LocalDate endDate,
                                          double monthlyTarget) {
        if (monthlyTarget <= 0) return 0.0;
        
        LocalDate monthStart = startDate.withDayOfMonth(1);
        LocalDate monthEnd = endDate.withDayOfMonth(endDate.lengthOfMonth());
        
        double monthlySales = payments.stream()
            .filter(p -> p.createdAt != null && 
                        !p.createdAt.toLocalDate().isBefore(monthStart) && 
                        !p.createdAt.toLocalDate().isAfter(monthEnd))
            .filter(p -> p.amount != null && p.amount > 0)
            .mapToDouble(p -> p.amount)
            .sum();
            
        return (monthlySales / monthlyTarget) * 100;
    }

    private double calculateAchievementRate(List<PaymentNotificationEntity> payments,
                                          LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return 0.0;
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long daysWithSales = payments.stream()
            .filter(p -> p.createdAt != null && 
                        !p.createdAt.toLocalDate().isBefore(startDate) && 
                        !p.createdAt.toLocalDate().isAfter(endDate))
            .filter(p -> p.amount != null && p.amount > 0)
            .map(p -> p.createdAt.toLocalDate())
            .distinct()
            .count();
            
        return daysBetween > 0 ? (double) daysWithSales / daysBetween * 100 : 0.0;
    }

    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments, 
                           LocalDate startDate, 
                           LocalDate endDate, 
                           Long adminId) {
        return payments != null; // Permitir listas vacías para calcular objetivos por defecto
    }

    @Override
    public String getStrategyName() {
        return "SellerGoalsStrategy";
    }
}
