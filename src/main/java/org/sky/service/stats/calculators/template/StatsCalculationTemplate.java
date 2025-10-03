package org.sky.service.stats.calculators.template;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.model.PaymentNotificationEntity;
import org.sky.service.stats.calculators.builder.StatsResultBuilder;
import org.sky.service.stats.calculators.factory.StatsCalculatorFactory;
import org.sky.service.stats.calculators.strategy.BasicStatsStrategy;
import org.sky.service.stats.calculators.strategy.DailySalesStrategy;
import org.sky.service.stats.calculators.strategy.HourlySalesStrategy;
import org.sky.service.stats.calculators.strategy.MonthlySalesStrategy;
import org.sky.service.stats.calculators.strategy.PerformanceMetricsStrategy;
import org.sky.service.stats.calculators.strategy.TopSellersStrategy;
import org.sky.service.stats.calculators.strategy.WeeklySalesStrategy;
import org.sky.service.stats.calculators.strategy.SellerGoalsStrategy;
import org.sky.service.stats.calculators.strategy.SellerPerformanceStrategy;
import org.sky.service.stats.calculators.strategy.SystemMetricsStrategy;
import org.sky.service.stats.calculators.strategy.FinancialOverviewStrategy;
import org.sky.service.stats.calculators.strategy.ComplianceSecurityStrategy;
import org.sky.service.stats.calculators.StatisticsCalculator.ParallelStatsResult;

import java.time.LocalDate;
import java.util.List;

/**
 * Template Method Pattern: Define el algoritmo común para cálculos de estadísticas
 * Clean Code: Principio de responsabilidad única - orquesta el proceso de cálculo
 */
@ApplicationScoped
public class StatsCalculationTemplate {
    
    private static final Logger log = Logger.getLogger(StatsCalculationTemplate.class);
    
    @Inject
    StatsCalculatorFactory calculatorFactory;
    
    @Inject
    BasicStatsStrategy basicStatsStrategy;
    
    @Inject
    PerformanceMetricsStrategy performanceMetricsStrategy;
    
    @Inject
    DailySalesStrategy dailySalesStrategy;
    
    @Inject
    HourlySalesStrategy hourlySalesStrategy;
    
    @Inject
    WeeklySalesStrategy weeklySalesStrategy;
    
    @Inject
    MonthlySalesStrategy monthlySalesStrategy;
    
    @Inject
    TopSellersStrategy topSellersStrategy;
    
    @Inject
    SellerGoalsStrategy sellerGoalsStrategy;
    
    @Inject
    SellerPerformanceStrategy sellerPerformanceStrategy;
    
    @Inject
    SystemMetricsStrategy systemMetricsStrategy;
    
    @Inject
    FinancialOverviewStrategy financialOverviewStrategy;
    
    @Inject
    ComplianceSecurityStrategy complianceSecurityStrategy;
    
    /**
     * Template Method: Define el algoritmo común para calcular todas las estadísticas
     */
    public Uni<ParallelStatsResult> calculateAllStats(List<PaymentNotificationEntity> payments, 
                                                    LocalDate startDate, 
                                                    LocalDate endDate, 
                                                    Long adminId) {
        
        log.info("🚀 StatsCalculationTemplate: Iniciando cálculos para " + payments.size() + " pagos");
        
        return Uni.combine()
            .all()
            .unis(
                calculateBasicStats(payments, startDate, endDate, adminId),
                calculatePerformanceMetrics(payments, startDate, endDate, adminId),
                calculateDailySales(payments, startDate, endDate, adminId),
                calculateHourlySales(payments, startDate, endDate, adminId),
                calculateWeeklySales(payments, startDate, endDate, adminId),
                calculateMonthlySales(payments, startDate, endDate, adminId),
                calculateTopSellers(payments, startDate, endDate, adminId),
                calculateSellerGoals(payments, startDate, endDate, adminId),
                calculateSellerPerformance(payments, startDate, endDate, adminId),
                calculateSystemMetrics(payments, startDate, endDate, adminId),
                calculateFinancialOverview(payments, startDate, endDate, adminId),
                calculateComplianceSecurity(payments, startDate, endDate, adminId)
            )
            .with(results -> 
                buildResult(results.get(0), results.get(1), results.get(2), results.get(3), results.get(4), results.get(5), 
                           results.get(6), results.get(7), results.get(8), results.get(9), results.get(10), results.get(11)))
            .onFailure().invoke(throwable -> {
                log.error("❌ StatsCalculationTemplate: Error en cálculos: " + throwable.getMessage());
                log.error("❌ StatsCalculationTemplate: Error type: " + throwable.getClass().getSimpleName());
                log.error("❌ StatsCalculationTemplate: Error stack trace: ", throwable);
            });
    }
    
    // Métodos específicos que pueden ser sobrescritos por subclases
    protected Uni<Object> calculateBasicStats(List<PaymentNotificationEntity> payments, 
                                            LocalDate startDate, 
                                            LocalDate endDate, 
                                            Long adminId) {
        return basicStatsStrategy.calculate(payments, startDate, endDate, adminId).map(basicStats -> (Object) basicStats);
    }
    
    protected Uni<Object> calculatePerformanceMetrics(List<PaymentNotificationEntity> payments, 
                                                    LocalDate startDate, 
                                                    LocalDate endDate, 
                                                    Long adminId) {
        return performanceMetricsStrategy.calculate(payments, startDate, endDate, adminId).map(performanceMetrics -> (Object) performanceMetrics);
    }
    
    protected Uni<Object> calculateDailySales(List<PaymentNotificationEntity> payments, 
                                            LocalDate startDate, 
                                            LocalDate endDate, 
                                            Long adminId) {
        return dailySalesStrategy.calculate(payments, startDate, endDate, adminId).map(dailySales -> (Object) dailySales);
    }
    
    protected Uni<Object> calculateHourlySales(List<PaymentNotificationEntity> payments, 
                                             LocalDate startDate, 
                                             LocalDate endDate, 
                                             Long adminId) {
        return hourlySalesStrategy.calculate(payments, startDate, endDate, adminId).map(hourlySales -> (Object) hourlySales);
    }
    
    protected Uni<Object> calculateWeeklySales(List<PaymentNotificationEntity> payments, 
                                             LocalDate startDate, 
                                             LocalDate endDate, 
                                             Long adminId) {
        return weeklySalesStrategy.calculate(payments, startDate, endDate, adminId).map(weeklySales -> (Object) weeklySales);
    }
    
    protected Uni<Object> calculateMonthlySales(List<PaymentNotificationEntity> payments, 
                                              LocalDate startDate, 
                                              LocalDate endDate, 
                                              Long adminId) {
        return monthlySalesStrategy.calculate(payments, startDate, endDate, adminId).map(monthlySales -> (Object) monthlySales);
    }
    
    protected Uni<Object> calculateTopSellers(List<PaymentNotificationEntity> payments, 
                                            LocalDate startDate, 
                                            LocalDate endDate, 
                                            Long adminId) {
        return topSellersStrategy.calculate(payments, startDate, endDate, adminId).map(topSellers -> (Object) topSellers);
    }
    
    protected Uni<Object> calculateSellerGoals(List<PaymentNotificationEntity> payments, 
                                             LocalDate startDate, 
                                             LocalDate endDate, 
                                             Long adminId) {
        return sellerGoalsStrategy.calculate(payments, startDate, endDate, adminId).map(sellerGoals -> (Object) sellerGoals);
    }
    
    protected Uni<Object> calculateSellerPerformance(List<PaymentNotificationEntity> payments, 
                                                   LocalDate startDate, 
                                                   LocalDate endDate, 
                                                   Long adminId) {
        return sellerPerformanceStrategy.calculate(payments, startDate, endDate, adminId).map(sellerPerformance -> (Object) sellerPerformance);
    }
    
    protected Uni<Object> calculateSystemMetrics(List<PaymentNotificationEntity> payments, 
                                               LocalDate startDate, 
                                               LocalDate endDate, 
                                               Long adminId) {
        return systemMetricsStrategy.calculate(payments, startDate, endDate, adminId).map(systemMetrics -> (Object) systemMetrics);
    }
    
    protected Uni<Object> calculateFinancialOverview(List<PaymentNotificationEntity> payments, 
                                                   LocalDate startDate, 
                                                   LocalDate endDate, 
                                                   Long adminId) {
        return financialOverviewStrategy.calculate(payments, startDate, endDate, adminId).map(financialOverview -> (Object) financialOverview);
    }
    
    protected Uni<Object> calculateComplianceSecurity(List<PaymentNotificationEntity> payments, 
                                                    LocalDate startDate, 
                                                    LocalDate endDate, 
                                                    Long adminId) {
        return complianceSecurityStrategy.calculate(payments, startDate, endDate, adminId).map(complianceSecurity -> (Object) complianceSecurity);
    }
    
    // Método final que construye el resultado
    @SuppressWarnings("unchecked")
    private ParallelStatsResult buildResult(Object basicStats, Object performanceMetrics, Object dailySales, 
                                          Object hourlySales, Object weeklySales, Object monthlySales, 
                                          Object topSellers, Object sellerGoals, Object sellerPerformance, 
                                          Object systemMetrics, Object financialOverview, Object complianceSecurity) {
        log.info("✅ StatsCalculationTemplate: Construyendo resultado final con 12 resultados");
        
        // Log de debugging para verificar que las estrategias se ejecutaron
        log.info("🔍 BasicStats: " + (basicStats != null ? basicStats.getClass().getSimpleName() : "null"));
        log.info("🔍 PerformanceMetrics: " + (performanceMetrics != null ? performanceMetrics.getClass().getSimpleName() : "null"));
        log.info("🔍 DailySales: " + (dailySales != null ? dailySales.getClass().getSimpleName() : "null"));
        log.info("🔍 HourlySales: " + (hourlySales != null ? hourlySales.getClass().getSimpleName() : "null"));
        log.info("🔍 WeeklySales: " + (weeklySales != null ? weeklySales.getClass().getSimpleName() : "null"));
        log.info("🔍 MonthlySales: " + (monthlySales != null ? monthlySales.getClass().getSimpleName() : "null"));
        log.info("🔍 TopSellers: " + (topSellers != null ? topSellers.getClass().getSimpleName() : "null"));
        log.info("🔍 SellerGoals: " + (sellerGoals != null ? sellerGoals.getClass().getSimpleName() : "null"));
        log.info("🔍 SellerPerformance: " + (sellerPerformance != null ? sellerPerformance.getClass().getSimpleName() : "null"));
        log.info("🔍 SystemMetrics: " + (systemMetrics != null ? systemMetrics.getClass().getSimpleName() : "null"));
        log.info("🔍 FinancialOverview: " + (financialOverview != null ? financialOverview.getClass().getSimpleName() : "null"));
        log.info("🔍 ComplianceSecurity: " + (complianceSecurity != null ? complianceSecurity.getClass().getSimpleName() : "null"));
        
        try {
            return StatsResultBuilder.newBuilder()
                .withBasicStats((org.sky.service.stats.calculators.StatisticsCalculator.BasicStats) basicStats)
                .withPerformanceMetrics((org.sky.service.stats.calculators.StatisticsCalculator.PerformanceMetrics) performanceMetrics)
                .withDailySales((java.util.List<org.sky.dto.response.stats.DailySalesData>) dailySales)
                .withHourlySales((java.util.List<org.sky.dto.response.stats.HourlySalesData>) hourlySales)
                .withWeeklySales((java.util.List<org.sky.dto.response.stats.WeeklySalesData>) weeklySales)
                .withMonthlySales((java.util.List<org.sky.dto.response.stats.MonthlySalesData>) monthlySales)
                .withTopSellers((java.util.List<org.sky.dto.response.stats.TopSellerData>) topSellers)
                .withSellerGoals((java.util.Map<String, Object>) sellerGoals)
                .withSellerPerformance((java.util.Map<String, Object>) sellerPerformance)
                .withSystemMetrics((java.util.Map<String, Object>) systemMetrics)
                .withFinancialOverview((java.util.Map<String, Object>) financialOverview)
                .withComplianceSecurity((java.util.Map<String, Object>) complianceSecurity)
                .build();
        } catch (Exception e) {
            log.error("❌ Error en buildResult: " + e.getMessage(), e);
            throw e;
        }
    }
}
