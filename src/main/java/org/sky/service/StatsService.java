package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.response.stats.AdminAnalyticsResponse;
import org.sky.service.analytics.StatsAggregatorService;

import java.time.LocalDate;
import java.util.Map;

/**
 * Servicio de estadísticas - Delegador principal
 * Responsabilidad única: Delegar a servicios especializados de analytics
 * 
 * @deprecated Este servicio se mantiene por compatibilidad. 
 * Usar directamente StatsAggregatorService para nuevas implementaciones.
 */
@ApplicationScoped
public class StatsService {
    
    private static final Logger log = Logger.getLogger(StatsService.class);
    
    @Inject
    StatsAggregatorService statsAggregatorService;
    
    // ==================================================================================
    // MÉTODOS DELEGADORES - Compatibilidad con código existente
    // ==================================================================================
    
    /**
     * Obtiene resumen completo de analytics para admin
     * @deprecated Usar StatsAggregatorService.getAnalyticsSummary() directamente
     */
    @Deprecated
    public Uni<AdminAnalyticsResponse> getAnalyticsSummary(Long adminId, LocalDate startDate, LocalDate endDate, 
                                                           String include, String period, String metric, 
                                                           String granularity, Double confidence, Integer days) {
        log.warn("⚠️ Usando método deprecated. Migrar a StatsAggregatorService.getAnalyticsSummary()");
        return statsAggregatorService.getAnalyticsSummary(adminId, startDate, endDate, include, period, metric, granularity, confidence, days);
    }
    
    /**
     * Obtiene analytics para admin
     * @deprecated Usar StatsAggregatorService.getAdminAnalytics() directamente
     */
    @Deprecated
    public Uni<AdminAnalyticsResponse> getAdminAnalytics(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.warn("⚠️ Usando método deprecated. Migrar a StatsAggregatorService.getAdminAnalytics()");
        return statsAggregatorService.getAdminAnalytics(adminId, startDate, endDate);
    }
    
    /**
     * Genera reporte de transparencia de pagos
     * @deprecated Usar StatsAggregatorService.generatePaymentTransparencyReport() directamente
     */
    @Deprecated
    public Uni<Map<String, Object>> generatePaymentTransparencyReport(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.warn("⚠️ Usando método deprecated. Migrar a StatsAggregatorService.generatePaymentTransparencyReport()");
        return statsAggregatorService.generatePaymentTransparencyReport(adminId, startDate, endDate);
    }
    
    /**
     * Obtiene estadísticas de admin (método requerido por StatsController)
     * @deprecated Usar StatsAggregatorService.getAdminStats() directamente
     */
    @Deprecated
    public Uni<Map<String, Object>> getAdminStats(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.warn("⚠️ Usando método deprecated. Migrar a StatsAggregatorService.getAdminStats()");
        return statsAggregatorService.getAdminStats(adminId, startDate, endDate);
    }

    /**
     * Obtiene estadísticas de seller (método requerido por StatsController)
     * @deprecated Usar StatsAggregatorService.getSellerStats() directamente
     */
    @Deprecated
    public Uni<Map<String, Object>> getSellerStats(Long sellerId, LocalDate startDate, LocalDate endDate) {
        log.warn("⚠️ Usando método deprecated. Migrar a StatsAggregatorService.getSellerStats()");
        return statsAggregatorService.getSellerStats(sellerId, startDate, endDate);
    }
    
    /**
     * Calcula métricas de crecimiento comparando con período anterior
     * @deprecated Usar PaymentAnalyticsService.calculateGrowthMetrics() directamente
     */
    @Deprecated
    public Uni<Map<String, Double>> calculateGrowthMetrics(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.warn("⚠️ Usando método deprecated. Migrar a PaymentAnalyticsService.calculateGrowthMetrics()");
        return statsAggregatorService.calculateGrowthMetrics(adminId, startDate, endDate);
    }
    
    /**
     * Obtiene el rol del usuario
     * @deprecated Usar AdminAnalyticsService.getUserRole() directamente
     */
    @Deprecated
    public Uni<String> getUserRole(Long userId) {
        log.warn("⚠️ Usando método deprecated. Migrar a AdminAnalyticsService.getUserRole()");
        return statsAggregatorService.getUserRole(userId);
    }
    
    /**
     * Valida que un seller pertenezca al usuario autenticado
     * @deprecated Usar SellerAnalyticsService.validateSellerOwnership() directamente
     */
    @Deprecated
    public Uni<Boolean> validateSellerOwnership(Long userId, Long sellerId) {
        log.warn("⚠️ Usando método deprecated. Migrar a SellerAnalyticsService.validateSellerOwnership()");
        return statsAggregatorService.validateSellerOwnership(userId, sellerId);
    }
}