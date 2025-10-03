package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.response.stats.*;
import org.sky.model.PaymentNotificationEntity;
import org.sky.service.stats.calculators.template.StatsCalculationTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Clean Code Refactored: StatisticsCalculator
 * 
 * Principios aplicados:
 * - Single Responsibility: Solo orquesta cálculos, delega a estrategias
 * - Open/Closed: Abierto para extensión, cerrado para modificación
 * - Dependency Inversion: Depende de abstracciones, no implementaciones
 * 
 * Patrones de diseño aplicados:
 * - Facade Pattern: Proporciona interfaz simplificada
 * - Delegation Pattern: Delega responsabilidades a componentes especializados
 */
@ApplicationScoped
public class StatisticsCalculator {

    private static final Logger log = Logger.getLogger(StatisticsCalculator.class);
    
    @Inject
    StatsCalculationTemplate calculationTemplate;
    
    /**
     * Facade Pattern: Proporciona interfaz simplificada para cálculos de estadísticas
     * Clean Code: Método con responsabilidad única - solo orquesta el cálculo
     */
    public Uni<ParallelStatsResult> calculateAllStatsInParallel(List<PaymentNotificationEntity> payments, 
                                                              LocalDate startDate, 
                                                              LocalDate endDate, 
                                                              Long adminId) {
        log.info("🚀 StatisticsCalculator: Delegando cálculo a template para " + payments.size() + " pagos");
        return calculationTemplate.calculateAllStats(payments, startDate, endDate, adminId);
    }

  // Clases de datos auxiliares - Value Objects
    public record BasicStats(double totalSales, long totalTransactions, double averageTransactionValue) {}
    
    public record PerformanceMetrics(double averageConfirmationTime, double claimRate, double rejectionRate,
                                   int pendingPayments, int confirmedPayments, int rejectedPayments) {}
    
    public record ParallelStatsResult(BasicStats basicStats, PerformanceMetrics performanceMetrics,
                                    List<DailySalesData> dailySales, List<HourlySalesData> hourlySales,
                                    List<WeeklySalesData> weeklySales, List<MonthlySalesData> monthlySales,
                                    List<TopSellerData> topSellers, Map<String, Object> sellerGoals,
                                    Map<String, Object> sellerPerformance, Map<String, Object> systemMetrics,
                                    Map<String, Object> financialOverview, Map<String, Object> complianceSecurity) {}
}