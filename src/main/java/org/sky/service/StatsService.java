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
import org.sky.service.stats.calculators.factory.CalculatorFactory;
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
    CalculatorFactory calculatorFactory;

    
    private static final Logger log = Logger.getLogger(StatsService.class);
    
    /**
     * Obtiene estadísticas generales para un admin
     */
    @WithTransaction
    public Uni<SalesStatsResponse> getAdminStats(Long adminId, LocalDate startDate, LocalDate endDate) {
        return calculatorFactory.getAdminStatsCalculator().calculateAdminStats(adminId, startDate, endDate)
                .map(adminStats -> new SalesStatsResponse(
                    new SalesStatsResponse.PeriodInfo(
                        adminStats.startDate().toString(),
                        adminStats.endDate().toString(),
                        (int) java.time.temporal.ChronoUnit.DAYS.between(adminStats.startDate(), adminStats.endDate())
                    ),
                    new SalesStatsResponse.SummaryStats(
                        adminStats.totalSales(),
                        adminStats.totalTransactions(),
                        adminStats.averageTransactionValue(),
                        0L, // pendingPayments
                        0L, // confirmedPayments
                        0L  // rejectedPayments
                    ),
                    java.util.List.of(), // dailyStats
                    java.util.List.of()  // sellerStats
                ));
    }
    
    /**
     * Obtiene estadísticas específicas para un vendedor
     */
    @WithTransaction
    public Uni<SellerStatsResponse> getSellerStats(Long sellerId, LocalDate startDate, LocalDate endDate) {
        return calculatorFactory.getSellerStatsCalculator().calculateSellerStats(sellerId, startDate, endDate);
    }

    /**
     * Obtiene resumen completo de analytics para admin
     */
    @WithTransaction
    public Uni<AnalyticsSummaryResponse> getAnalyticsSummary(Long adminId, LocalDate startDate, LocalDate endDate, 
                                                           String include, String period, String metric, 
                                                           String granularity, Double confidence, Integer days) {
        var request = new AdminAnalyticsRequest(adminId, startDate, endDate, include, period, metric, granularity, confidence, days);
        return calculatorFactory.getAdminAnalyticsCalculator().calculateAnalyticsSummary(request);
    }
    
    /**
     * Obtiene resumen rápido para dashboard
     */
    @WithTransaction
    public Uni<QuickSummaryResponse> getQuickSummary(Long adminId, LocalDate startDate, LocalDate endDate) {
        return calculatorFactory.getQuickSummaryCalculator().calculateQuickSummary(adminId, startDate, endDate);
    }

    /**
     * Obtiene analytics completos para un vendedor específico
     */
    @WithTransaction
    public Uni<SellerAnalyticsResponse> getSellerAnalyticsSummary(Long sellerId, LocalDate startDate, LocalDate endDate,
                                                                String include, String period, String metric,
                                                                String granularity, Double confidence, Integer days) {
        var request = new SellerAnalyticsRequest(sellerId, startDate, endDate, include, period, metric, granularity, confidence, days);
        return calculatorFactory.getSellerAnalyticsCalculator().calculateSellerAnalyticsSummary(request);
    }


    @WithTransaction
    public Uni<FinancialAnalyticsResponse> getFinancialAnalytics(Long adminId, LocalDate startDate, LocalDate endDate, 
                                            String include, String currency, Double taxRate) {
        var request = new FinancialAnalyticsRequest(adminId, startDate, endDate, include, currency, 0.05, taxRate);
        return calculatorFactory.getFinancialAnalyticsCalculator().calculateFinancialAnalytics(request);
    }
    
    /**
     * Obtiene análisis financiero específico para vendedor
     */
    @WithTransaction
    public Uni<SellerFinancialResponse> getSellerFinancialAnalytics(Long sellerId, LocalDate startDate, LocalDate endDate,
                                                  String include, String currency, Double commissionRate) {
        log.info("💰 StatsService.getSellerFinancialAnalytics() - Delegando a SellerFinancialCalculator");
        return calculatorFactory.getSellerFinancialCalculator().calculateSellerFinancialAnalytics(sellerId, startDate, endDate, include, currency, commissionRate);
    }
    
    /**
     * Obtiene reporte de transparencia de pagos
     */
    @WithTransaction
    public Uni<PaymentTransparencyResponse> getPaymentTransparencyReport(Long adminId, LocalDate startDate, LocalDate endDate,
                                                   Boolean includeFees, Boolean includeTaxes, Boolean includeCommissions) {
        var request = new PaymentTransparencyRequest(adminId, startDate, endDate, includeFees, includeTaxes, includeCommissions);
        return calculatorFactory.getPaymentTransparencyCalculator().calculatePaymentTransparencyReport(request);
    }
    

}
