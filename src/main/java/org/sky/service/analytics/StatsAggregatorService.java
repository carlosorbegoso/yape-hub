package org.sky.service.analytics;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.response.stats.*;
import org.sky.dto.response.admin.*;
import org.sky.dto.response.seller.*;
import org.sky.dto.response.branch.*;
import org.sky.service.analytics.PaymentAnalyticsService.PaymentMetrics;
import org.sky.service.hubnotifications.PaymentNotificationService;
import org.sky.service.stats.calculators.StatisticsCalculator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Servicio coordinador que agrega todas las métricas de analytics
 * Responsabilidad única: Coordinar y agregar resultados de otros servicios especializados
 */
@ApplicationScoped
public class StatsAggregatorService {
    
    private static final Logger log = Logger.getLogger(StatsAggregatorService.class);
    
    @Inject
    PaymentAnalyticsService paymentAnalyticsService;
    
    @Inject
    SellerAnalyticsService sellerAnalyticsService;
    
    @Inject
    FinancialAnalyticsService financialAnalyticsService;
    
    @Inject
    AdminAnalyticsService adminAnalyticsService;
    
    @Inject
    StatisticsCalculator statisticsCalculator;
    
    @Inject
    PaymentNotificationService paymentNotificationService;
    
    /**
     * Obtiene resumen completo de analytics para admin con programación reactiva pura
     */
    @WithTransaction
    public Uni<AdminAnalyticsResponse> getAnalyticsSummary(Long adminId, LocalDate startDate, LocalDate endDate, 
                                                           String include, String period, String metric, 
                                                           String granularity, Double confidence, Integer days) {
        log.info("📊 StatsAggregatorService.getAnalyticsSummary() - AdminId: " + adminId);
        
        return paymentAnalyticsService.calculatePaymentMetrics(adminId, startDate, endDate)
            .chain(paymentMetrics -> {
                return statisticsCalculator.calculateAllStatsInParallel(
                    List.of(), // Payments ya procesados en PaymentAnalyticsService
                    startDate, endDate, adminId
                ).map(result -> buildCompleteAnalyticsResponse(
                    paymentMetrics, result, endDate, confidence
                ));
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("❌ Error obteniendo analytics: " + throwable.getMessage());
                return AdminAnalyticsResponse.empty();
            });
    }
    
    /**
     * Obtiene analytics para admin (método requerido por AdminBillingController)
     */
    @WithTransaction
    public Uni<AdminAnalyticsResponse> getAdminAnalytics(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 StatsAggregatorService.getAdminAnalytics() - Obteniendo datos reales para adminId: " + adminId);
        
        return paymentAnalyticsService.calculatePaymentMetrics(adminId, startDate, endDate)
            .chain(paymentMetrics -> {
                return statisticsCalculator.calculateAllStatsInParallel(
                    List.of(), // Payments ya procesados
                    startDate, endDate, adminId
                ).map(result -> buildCompleteAnalyticsResponse(
                    paymentMetrics, result, endDate, null
                ));
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("❌ Error obteniendo analytics: " + throwable.getMessage());
                return AdminAnalyticsResponse.empty();
            });
    }
    
    /**
     * Genera reporte de transparencia de pagos
     */
    @WithTransaction
    public Uni<Map<String, Object>> generatePaymentTransparencyReport(Long adminId, LocalDate startDate, LocalDate endDate) {
        return paymentAnalyticsService.generatePaymentTransparencyReport(adminId, startDate, endDate);
    }
    
    /**
     * Obtiene estadísticas de admin (método requerido por StatsController)
     */
    public Uni<Map<String, Object>> getAdminStats(Long adminId, LocalDate startDate, LocalDate endDate) {
        return getAdminAnalytics(adminId, startDate, endDate)
            .map(response -> Map.<String, Object>of(
                "overview", response.overview(),
                "dailySales", response.dailySales(),
                "topSellers", response.topSellers(),
                "performanceMetrics", response.performanceMetrics()
            ));
    }

    /**
     * Obtiene estadísticas de seller (método requerido por StatsController)
     */
    public Uni<Map<String, Object>> getSellerStats(Long sellerId, LocalDate startDate, LocalDate endDate) {
        return getAnalyticsSummary(sellerId, startDate, endDate, null, null, null, null, null, null)
            .map(response -> Map.<String, Object>of(
                "overview", response.overview(),
                "dailySales", response.dailySales(),
                "performanceMetrics", response.performanceMetrics()
            ));
    }
    
    /**
     * Calcula métricas de crecimiento comparando con período anterior
     */
    public Uni<Map<String, Double>> calculateGrowthMetrics(Long adminId, LocalDate startDate, LocalDate endDate) {
        return paymentAnalyticsService.calculateGrowthMetrics(adminId, startDate, endDate);
    }
    
    /**
     * Obtiene el rol del usuario delegando al PaymentNotificationService
     */
    public Uni<String> getUserRole(Long userId) {
        return adminAnalyticsService.getUserRole(userId, paymentNotificationService);
    }
    
    /**
     * Valida que un seller pertenezca al usuario autenticado
     */
    public Uni<Boolean> validateSellerOwnership(Long userId, Long sellerId) {
        return sellerAnalyticsService.validateSellerOwnership(userId, sellerId);
    }
    
    // ==================================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // ==================================================================================
    
    /**
     * Construye la respuesta completa de analytics agregando todos los servicios
     */
    private AdminAnalyticsResponse buildCompleteAnalyticsResponse(
            PaymentMetrics paymentMetrics,
            StatisticsCalculator.ParallelStatsResult result,
            LocalDate endDate,
            Double confidence) {
        
        // Generar métricas de overview
        OverviewMetrics overview = new OverviewMetrics(
            paymentMetrics.totalSales(), 
            paymentMetrics.totalTransactions(), 
            paymentMetrics.averageTransactionValue(), 
            0.0, 0.0, 0.0,
            paymentMetrics.allSales(),
            paymentMetrics.confirmedTransactions(),
            paymentMetrics.pendingTransactions(),
            paymentMetrics.rejectedTransactions()
        );
        
        // Generar métricas de performance usando FinancialAnalyticsService
        PerformanceMetrics performanceMetrics = financialAnalyticsService.generatePerformanceMetrics(paymentMetrics);
        
        // Generar métricas del sistema usando FinancialAnalyticsService
        SystemMetrics systemMetrics = financialAnalyticsService.generateSystemMetrics(paymentMetrics);
        
        // Generar datos financieros
        Map<String, Object> financialOverviewData = result.financialOverview();
        FinancialOverview financialOverview = financialAnalyticsService.convertToFinancialOverview(
            financialOverviewData, paymentMetrics.totalSales()
        );
        
        // Generar compliance y seguridad
        ComplianceAndSecurity complianceAndSecurity = financialAnalyticsService.generateComplianceAndSecurity();
        
        // Usar datos calculados en paralelo del StatisticsCalculator
        List<DailySalesData> dailySales = result.dailySales();
        List<HourlySalesData> hourlySales = result.hourlySales();
        List<WeeklySalesData> weeklySales = result.weeklySales();
        List<MonthlySalesData> monthlySales = result.monthlySales();
        List<TopSellerData> topSellers = result.topSellers();
        
        // Generar datos de vendedores usando SellerAnalyticsService
        SellerTrends sellerTrends = sellerAnalyticsService.generateSellerTrends(paymentMetrics);
        SellerAchievements sellerAchievements = sellerAnalyticsService.generateSellerAchievements(paymentMetrics, endDate);
        SellerInsights sellerInsights = sellerAnalyticsService.generateSellerInsights(paymentMetrics);
        SellerForecasting sellerForecasting = sellerAnalyticsService.generateSellerForecasting(paymentMetrics);
        SellerAnalytics sellerAnalytics = sellerAnalyticsService.generateSellerAnalytics(paymentMetrics);
        
        // Aplicar filtro de confianza si está especificado
        if (confidence != null && sellerForecasting != null) {
            sellerForecasting = applyConfidenceFilter(sellerForecasting, confidence);
        }
        
        // Generar datos administrativos usando AdminAnalyticsService
        SellerComparisons sellerComparisons = adminAnalyticsService.generateSellerComparisons(paymentMetrics);
        BranchAnalytics branchAnalytics = adminAnalyticsService.generateBranchAnalytics(paymentMetrics);
        SellerManagement sellerManagement = adminAnalyticsService.generateSellerManagement(paymentMetrics);
        AdministrativeInsights administrativeInsights = adminAnalyticsService.generateAdministrativeInsights(paymentMetrics, endDate);
        
        // Convertir datos de estrategias
        Map<String, Object> sellerGoalsData = result.sellerGoals();
        Map<String, Object> sellerPerformanceData = result.sellerPerformance();
        SellerGoals sellerGoals = adminAnalyticsService.convertToSellerGoals(sellerGoalsData);
        SellerPerformance sellerPerformance = adminAnalyticsService.convertToSellerPerformance(sellerPerformanceData);
        
        return AdminAnalyticsResponse.createComplete(
            overview,
            dailySales,
            topSellers,
            performanceMetrics,
            hourlySales,
            weeklySales,
            monthlySales,
            sellerGoals,
            sellerPerformance,
            sellerComparisons,
            sellerTrends,
            sellerAchievements,
            sellerInsights,
            sellerForecasting,
            sellerAnalytics,
            branchAnalytics,
            sellerManagement,
            systemMetrics,
            administrativeInsights,
            financialOverview,
            complianceAndSecurity
        );
    }
    
    /**
     * Aplica filtro de confianza a las predicciones
     */
    private SellerForecasting applyConfidenceFilter(SellerForecasting sellerForecasting, Double confidence) {
        if (confidence == null || sellerForecasting == null) {
            return sellerForecasting;
        }
        
        // Filtrar predicciones por nivel de confianza
        List<PredictedSale> originalPredictions = sellerForecasting.predictedSales();
        List<PredictedSale> filteredPredictions = originalPredictions.stream()
            .filter(prediction -> prediction.confidence() >= confidence)
            .toList();
        
        // Log para debugging
        if (filteredPredictions.size() < originalPredictions.size()) {
            log.info("🔍 Confidence filter removed " + (originalPredictions.size() - filteredPredictions.size()) + 
                    " predictions (confidence threshold: " + confidence + ")");
        }
        
        return new SellerForecasting(
            filteredPredictions,
            sellerForecasting.trendAnalysis(),
            sellerForecasting.recommendations()
        );
    }
}

