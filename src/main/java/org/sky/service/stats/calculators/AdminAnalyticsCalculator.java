package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.AdminAnalyticsRequest;
import org.sky.dto.stats.AnalyticsSummaryResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.calculators.admin.overview.AdminOverviewCalculator;
import org.sky.service.stats.calculators.admin.sales.AdminSalesCalculator;
import org.sky.service.stats.calculators.admin.performance.AdminPerformanceCalculator;
import org.sky.service.stats.calculators.admin.sellers.AdminSellersCalculator;
import org.sky.service.stats.calculators.admin.insights.AdminInsightsCalculator;
import org.sky.service.stats.calculators.admin.management.AdminManagementCalculator;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.util.List;

@ApplicationScoped
public class AdminAnalyticsCalculator {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    AdminOverviewCalculator overviewCalculator;
    
    @Inject
    AdminSalesCalculator salesCalculator;
    
    @Inject
    AdminPerformanceCalculator performanceCalculator;
    
    @Inject
    AdminSellersCalculator sellersCalculator;
    
    @Inject
    AdminInsightsCalculator insightsCalculator;
    
    @Inject
    AdminManagementCalculator managementCalculator;
    
    @WithTransaction
    public Uni<AnalyticsSummaryResponse> calculateAnalyticsSummary(AdminAnalyticsRequest request) {
        return paymentNotificationRepository.findPaymentsForAnalytics(
                request.adminId(), request.startDate().atStartOfDay(), request.endDate().atTime(23, 59, 59))
                .chain(payments -> sellerRepository.findActiveSellersByAdminId(request.adminId())
                        .chain(sellers -> buildAnalyticsSummaryResponse(payments, sellers, request)));
    }
    
    private Uni<AnalyticsSummaryResponse> buildAnalyticsSummaryResponse(List<PaymentNotification> payments, 
                                                                       List<Seller> sellers, 
                                                                       AdminAnalyticsRequest request) {
        // Calcular métricas de resumen (usar period y granularity)
        var overview = overviewCalculator.calculateOverviewMetrics(payments, request.startDate(), request.endDate());
        
        // Calcular ventas diarias (usar granularity para determinar el tipo de datos)
        var dailySales = salesCalculator.calculateDailySalesData(payments, request.startDate(), request.endDate());
        
        // Calcular top vendedores (usar metric para determinar el criterio de ranking)
        var topSellers = sellersCalculator.calculateTopSellers(payments, sellers);
        
        // Calcular métricas de rendimiento (usar confidence para cálculos estadísticos)
        var performance = performanceCalculator.calculatePerformanceMetrics(payments);
        
        // Calcular datos avanzados para admin analytics (usar period y granularity)
        var hourlySales = salesCalculator.calculateHourlySalesData(payments, request.startDate(), request.endDate());
        var weeklySales = salesCalculator.calculateWeeklySalesData(payments, request.startDate(), request.endDate());
        var monthlySales = salesCalculator.calculateMonthlySalesData(payments, request.startDate(), request.endDate());
        
        // Calcular métricas avanzadas para admin (usar days para ventanas de tiempo)
        var goals = performanceCalculator.calculateGoals(payments, request.startDate(), request.endDate());
        var sellerPerformance = performanceCalculator.calculateSellerPerformance(payments, sellers, request.startDate(), request.endDate());
        var comparisons = sellersCalculator.calculateComparisons(payments, request.startDate(), request.endDate());
        var trends = sellersCalculator.calculateTrends(payments, request.startDate(), request.endDate());
        var achievements = sellersCalculator.calculateAchievements(payments, sellers, request.startDate(), request.endDate());
        var insights = insightsCalculator.calculateInsights(payments, sellers, request.startDate(), request.endDate());
        var forecasting = insightsCalculator.calculateForecasting(payments, request.startDate(), request.endDate());
        var analytics = insightsCalculator.calculateAnalytics(payments, sellers, request.startDate(), request.endDate());
        
        // Calcular nuevos campos avanzados usando Uni (usar include para determinar qué incluir)
        return Uni.combine().all().unis(
            managementCalculator.calculateBranchAnalytics(payments, sellers, request.startDate(), request.endDate()),
            managementCalculator.calculateSellerManagement(payments, sellers, request.startDate(), request.endDate()),
            managementCalculator.calculateSystemMetrics(payments, sellers, request.startDate(), request.endDate()),
            managementCalculator.calculateAdministrativeInsights(payments, sellers, request.startDate(), request.endDate()),
            managementCalculator.calculateFinancialOverview(payments, sellers, request.startDate(), request.endDate()),
            managementCalculator.calculateComplianceAndSecurity(payments, sellers, request.startDate(), request.endDate())
        ).with((branchAnalytics, sellerManagement, systemMetrics, 
                administrativeInsights, financialOverview, complianceAndSecurity) -> 
            new AnalyticsSummaryResponse(
                overview, dailySales, hourlySales, weeklySales, monthlySales, 
                topSellers, performance, goals, sellerPerformance, comparisons, 
                trends, achievements, insights, forecasting, analytics,
                branchAnalytics, sellerManagement, systemMetrics, 
                administrativeInsights, financialOverview, complianceAndSecurity
            )
        );
    }
}
