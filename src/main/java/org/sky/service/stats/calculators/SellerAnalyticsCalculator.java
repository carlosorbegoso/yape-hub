package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.SellerAnalyticsRequest;
import org.sky.dto.stats.SellerAnalyticsResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
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
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.util.List;

@ApplicationScoped
public class SellerAnalyticsCalculator {
    
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
        return sellerRepository.findById(request.sellerId())
                .onItem().ifNull().failWith(() -> new RuntimeException("Vendedor no encontrado"))
                .chain(seller -> {
                    // Ejecutar consultas en paralelo para aprovechar Mutiny
                    var paymentsUni = paymentNotificationRepository.findByAdminIdAndDateRange(
                            seller.branch.admin.id, request.startDate().atStartOfDay(), request.endDate().atTime(23, 59, 59));
                    
                    return paymentsUni.map(payments -> buildSellerAnalyticsResponse(seller, payments, request));
                });
    }
    
    private SellerAnalyticsResponse buildSellerAnalyticsResponse(Seller seller, List<PaymentNotification> payments, 
                                                               SellerAnalyticsRequest request) {
        // Filtrar pagos del vendedor específico
        var sellerPayments = filterPaymentsBySeller(payments, request.sellerId());
        
        // Calcular métricas usando todos los parámetros
        var overview = overviewCalculator.calculateOverviewMetrics(sellerPayments, payments, request);
        var dailySales = dailyCalculator.calculateDailySalesData(sellerPayments, payments, request);
        var performance = performanceCalculator.calculatePerformanceMetrics(sellerPayments, request);
        
        // Crear datos usando todos los parámetros
        var hourlySales = List.<SellerAnalyticsResponse.HourlySalesData>of();
        var weeklySales = List.<SellerAnalyticsResponse.WeeklySalesData>of();
        var monthlySales = List.<SellerAnalyticsResponse.MonthlySalesData>of();
        var goals = goalsCalculator.calculateSellerGoals(sellerPayments, payments, request);
        var sellerPerformance = performanceCalculator.calculateSellerPerformance(sellerPayments, payments, request);
        var comparisons = comparisonsCalculator.calculateSellerComparisons(sellerPayments, payments, request);
        var trends = trendsCalculator.calculateSellerTrends(sellerPayments, payments, request);
        var achievements = achievementsCalculator.calculateSellerAchievements(sellerPayments, payments, request);
        var insights = insightsCalculator.calculateSellerInsights(sellerPayments, payments, request);
        var forecasting = forecastingCalculator.calculateSellerForecasting(sellerPayments, payments, request);
        var analytics = analyticsDataCalculator.calculateSellerAnalytics(sellerPayments, payments, request);
        
        return new SellerAnalyticsResponse(
                overview, dailySales, hourlySales, weeklySales, monthlySales,
                performance, goals, sellerPerformance, comparisons,
                trends, achievements, insights, forecasting, analytics
        );
    }
    
    private List<PaymentNotification> filterPaymentsBySeller(List<PaymentNotification> payments, Long sellerId) {
        return payments.stream()
                .filter(payment -> sellerId.equals(payment.confirmedBy))
                .toList();
    }
    
}
