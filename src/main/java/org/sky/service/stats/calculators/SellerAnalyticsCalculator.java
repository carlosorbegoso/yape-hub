package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.*;
import org.sky.model.PaymentNotificationEntity;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.calculators.seller.overview.SellerOverviewCalculator;
import org.sky.service.stats.calculators.seller.daily.SellerDailyCalculator;
import org.sky.service.stats.calculators.seller.performance.SellerPerformanceCalculator;
import org.sky.service.stats.calculators.seller.trends.SellerTrendsCalculator;
import org.sky.service.stats.calculators.seller.goals.SellerGoalsCalculator;
import org.sky.service.stats.calculators.seller.insights.SellerInsightsCalculator;
import org.sky.service.stats.calculators.seller.forecasting.SellerForecastingCalculator;
import org.sky.service.stats.calculators.seller.comparisons.SellerComparisonsCalculator;
import org.sky.service.stats.calculators.seller.achievements.SellerAchievementsCalculator;
import org.sky.service.stats.calculators.seller.analytics.SellerAnalyticsDataCalculator;
import org.sky.service.stats.calculators.template.BaseStatsCalculator;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.util.List;

@ApplicationScoped
public class SellerAnalyticsCalculator extends BaseStatsCalculator<SellerAnalyticsRequest, SellerAnalyticsResponse> {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    SellerOverviewCalculator overviewCalculator;
    
    @Inject
    SellerDailyCalculator dailyCalculator;
    
    @Inject
    SellerPerformanceCalculator performanceCalculator;
    
    @Inject
    SellerTrendsCalculator trendsCalculator;
    
    @Inject
    SellerGoalsCalculator goalsCalculator;
    
    @Inject
    SellerInsightsCalculator insightsCalculator;
    
    @Inject
    SellerForecastingCalculator forecastingCalculator;
    
    @Inject
    SellerComparisonsCalculator comparisonsCalculator;
    
    @Inject
    SellerAchievementsCalculator achievementsCalculator;
    
    @Inject
    SellerAnalyticsDataCalculator analyticsDataCalculator;
    
    @WithTransaction
    public Uni<SellerAnalyticsResponse> calculateSellerAnalyticsSummary(SellerAnalyticsRequest request) {
        var templateRequest = new SellerAnalyticsRequest(
            request.sellerId(),
            request.startDate(),
            request.endDate(),
            request.include(),
            request.period(),
            request.metric(),
            request.granularity(),
            request.confidence(),
            request.days()
        );
        
        return sellerRepository.findById(request.sellerId())
                .onItem().ifNull().failWith(() -> new RuntimeException("Vendedor no encontrado"))
                .chain(seller -> {
                    // Ejecutar consultas en paralelo para aprovechar Mutiny
                    var paymentsUni = paymentNotificationRepository.findByAdminIdAndDateRange(
                            seller.branch.admin.id, request.startDate().atStartOfDay(), request.endDate().atTime(23, 59, 59));
                    
                    return paymentsUni.map(payments -> calculateStats(payments, templateRequest));
                });
    }
    
    @Override
    protected void validateInput(List<PaymentNotificationEntity> payments, SellerAnalyticsRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        if (payments == null) {
            throw new IllegalArgumentException("Los pagos no pueden ser null");
        }
    }
    
    @Override
    protected List<PaymentNotificationEntity> filterPayments(List<PaymentNotificationEntity> payments, SellerAnalyticsRequest request) {
        // Para seller analytics, filtramos por vendedor específico
        return payments.stream()
                .filter(payment -> request.sellerId().equals(payment.confirmedBy))
                .toList();
    }
    
    @Override
    protected Object calculateSpecificMetrics(List<PaymentNotificationEntity> payments, SellerAnalyticsRequest request) {
        // Métricas específicas para seller analytics: cálculos complejos
        Object overview = null;
        Object dailySales = null;
        Object performance = null;
        
        return new SellerAnalyticsSpecificMetrics(
            overview, dailySales, performance
        );
    }
    
    @Override
    protected SellerAnalyticsResponse buildResponse(Double totalSales, Long totalTransactions, 
                                                  Double averageTransactionValue, Double claimRate,
                                                  Object specificMetrics, List<PaymentNotificationEntity> payments,
                                                  SellerAnalyticsRequest request) {
        var sellerAnalyticsMetrics = (SellerAnalyticsSpecificMetrics) specificMetrics;
        
        // Para evitar bloqueo, usamos los pagos ya filtrados
        // En un entorno reactivo, la validación del seller se haría en el endpoint
        var allPayments = payments;
        
        Object dtoRequest = null;
        
        // Calcular métricas adicionales
        var hourlySales = List.<HourlySalesData>of();
        var weeklySales = List.<WeeklySalesData>of();
        var monthlySales = List.<MonthlySalesData>of();
        SellerGoals goals = null;
        Object sellerPerformance = null;
        Object comparisons = null;
        Object trends = null;
        Object achievements = null;
        Object insights = null;
        Object forecasting = null;
        SellerAnalytics analytics = null;
        
        return new SellerAnalyticsResponse(
            null, null, hourlySales, weeklySales, monthlySales,
            null, goals, null, null,
            null, null, null, null, analytics
        );
    }
    
    /**
     * Métricas específicas para seller analytics
     */
    private record SellerAnalyticsSpecificMetrics(
        Object overview,
        Object dailySales,
        Object performance
    ) {}
    
}
