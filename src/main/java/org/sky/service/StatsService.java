package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.stats.SalesStatsResponse;
import org.sky.dto.stats.SellerAnalyticsResponse;
import org.sky.dto.stats.SellerStatsResponse;
import org.sky.dto.stats.AnalyticsSummaryResponse;
import org.sky.dto.stats.QuickSummaryResponse;
import org.sky.dto.stats.SellerFinancialResponse;
import org.sky.dto.stats.FinancialAnalyticsRequest;
import org.sky.dto.stats.FinancialAnalyticsResponse;
import org.sky.dto.stats.PaymentTransparencyRequest;
import org.sky.dto.stats.PaymentTransparencyResponse;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.calculators.SellerFinancialCalculator;
import org.sky.service.stats.calculators.AdminStatsCalculator;
import org.sky.service.stats.calculators.SellerStatsCalculator;
import org.sky.service.stats.calculators.AdminAnalyticsCalculator;
import org.sky.service.stats.calculators.QuickSummaryCalculator;
import org.sky.service.stats.calculators.SellerAnalyticsCalculator;
import org.sky.service.stats.calculators.FinancialAnalyticsCalculator;
import org.sky.service.stats.calculators.PaymentTransparencyCalculator;
import org.sky.dto.stats.AdminAnalyticsRequest;
import org.sky.dto.stats.SellerAnalyticsRequest;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;

@ApplicationScoped
public class StatsService {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    SellerFinancialCalculator sellerFinancialCalculator;
    
    @Inject
    AdminStatsCalculator adminStatsCalculator;
    
    @Inject
    SellerStatsCalculator sellerStatsCalculator;
    
    @Inject
    AdminAnalyticsCalculator adminAnalyticsCalculator;
    
    @Inject
    QuickSummaryCalculator quickSummaryCalculator;
    
    @Inject
    SellerAnalyticsCalculator sellerAnalyticsCalculator;
    
    @Inject
    FinancialAnalyticsCalculator financialAnalyticsCalculator;
    
    @Inject
    PaymentTransparencyCalculator paymentTransparencyCalculator;

    
    private static final Logger log = Logger.getLogger(StatsService.class);
    
    /**
     * Obtiene estadísticas generales para un admin
     */
    @WithTransaction
    public Uni<SalesStatsResponse> getAdminStats(Long adminId, LocalDate startDate, LocalDate endDate) {
        return adminStatsCalculator.calculateAdminStats(adminId, startDate, endDate);
    }
    
    /**
     * Obtiene estadísticas específicas para un vendedor
     */
    @WithTransaction
    public Uni<SellerStatsResponse> getSellerStats(Long sellerId, LocalDate startDate, LocalDate endDate) {
        return sellerStatsCalculator.calculateSellerStats(sellerId, startDate, endDate);
    }

    /**
     * Obtiene resumen completo de analytics para admin
     */
    @WithTransaction
    public Uni<AnalyticsSummaryResponse> getAnalyticsSummary(Long adminId, LocalDate startDate, LocalDate endDate, 
                                                           String include, String period, String metric, 
                                                           String granularity, Double confidence, Integer days) {
        var request = new AdminAnalyticsRequest(adminId, startDate, endDate, include, period, metric, granularity, confidence, days);
        return adminAnalyticsCalculator.calculateAnalyticsSummary(request);
    }
    
    /**
     * Obtiene resumen rápido para dashboard
     */
    @WithTransaction
    public Uni<QuickSummaryResponse> getQuickSummary(Long adminId, LocalDate startDate, LocalDate endDate) {
        return quickSummaryCalculator.calculateQuickSummary(adminId, startDate, endDate);
    }

  /**
     * Obtiene analytics completos para un vendedor específico
     */
    @WithTransaction
    public Uni<SellerAnalyticsResponse> getSellerAnalyticsSummary(Long sellerId, LocalDate startDate, LocalDate endDate,
                                                                String include, String period, String metric,
                                                                String granularity, Double confidence, Integer days) {
        var request = new SellerAnalyticsRequest(sellerId, startDate, endDate, include, period, metric, granularity, confidence, days);
        return sellerAnalyticsCalculator.calculateSellerAnalyticsSummary(request);
    }


    @WithTransaction
    public Uni<FinancialAnalyticsResponse> getFinancialAnalytics(Long adminId, LocalDate startDate, LocalDate endDate, 
                                            String include, String currency, Double taxRate) {
        var request = new FinancialAnalyticsRequest(adminId, startDate, endDate, include, currency, taxRate);
        return financialAnalyticsCalculator.calculateFinancialAnalytics(request);
    }
    
    /**
     * Obtiene análisis financiero específico para vendedor
     */
    @WithTransaction
    public Uni<SellerFinancialResponse> getSellerFinancialAnalytics(Long sellerId, LocalDate startDate, LocalDate endDate,
                                                  String include, String currency, Double commissionRate) {
        log.info("💰 StatsService.getSellerFinancialAnalytics() - Delegando a SellerFinancialCalculator");
        return sellerFinancialCalculator.calculateSellerFinancialAnalytics(sellerId, startDate, endDate, include, currency, commissionRate);
    }
    
    /**
     * Obtiene reporte de transparencia de pagos
     */
    @WithTransaction
    public Uni<PaymentTransparencyResponse> getPaymentTransparencyReport(Long adminId, LocalDate startDate, LocalDate endDate,
                                                   Boolean includeFees, Boolean includeTaxes, Boolean includeCommissions) {
        var request = new PaymentTransparencyRequest(adminId, startDate, endDate, includeFees, includeTaxes, includeCommissions);
        return paymentTransparencyCalculator.calculatePaymentTransparencyReport(request);
    }
    

}
