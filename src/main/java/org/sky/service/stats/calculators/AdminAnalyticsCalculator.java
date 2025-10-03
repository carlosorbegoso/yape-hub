package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.request.admin.AdminAnalyticsRequest;
import org.sky.dto.response.admin.*;
import org.sky.dto.response.branch.BranchAnalytics;
import org.sky.dto.response.seller.SellerManagement;
import org.sky.dto.response.stats.*;

import org.sky.model.PaymentNotificationEntity;
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
public class AdminAnalyticsCalculator extends BaseStatsCalculator<AdminAnalyticsRequest, AnalyticsSummaryResponse> {
    
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
                        .chain(sellers -> buildResponseReactive(payments, request, sellers)));
    }
    
    @Override
    protected void validateInput(List<PaymentNotificationEntity> payments,  AdminAnalyticsRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        if (payments == null) {
            throw new IllegalArgumentException("Los pagos no pueden ser null");
        }
    }
    
    @Override
    protected List<PaymentNotificationEntity> filterPayments(List<PaymentNotificationEntity> payments,  AdminAnalyticsRequest request) {
        // Para admin analytics, no filtramos por vendedor específico
        return payments;
    }
    
    @Override
    protected Object calculateSpecificMetrics(List<PaymentNotificationEntity> payments,  AdminAnalyticsRequest request) {
        // Calcular métricas específicas usando los calculadores inyectados
        var overview = overviewCalculator.calculateOverviewMetrics(payments, request.startDate(), request.endDate());
        var dailySales = salesCalculator.calculateDailySalesData(payments, request.startDate(), request.endDate());
        var hourlySales = salesCalculator.calculateHourlySalesData(payments, request.startDate(), request.endDate());
        var weeklySales = salesCalculator.calculateWeeklySalesData(payments, request.startDate(), request.endDate());
        var monthlySales = salesCalculator.calculateMonthlySalesData(payments, request.startDate(), request.endDate());
        var performanceMetrics = performanceCalculator.calculatePerformanceMetrics(payments);
        
        return new AdminAnalyticsSpecificMetrics(
            overview, dailySales, hourlySales, weeklySales, monthlySales, performanceMetrics
        );
    }
    
    @Override
    protected AnalyticsSummaryResponse buildResponse(Double totalSales, Long totalTransactions, 
                                                   Double averageTransactionValue, Double claimRate,
                                                   Object specificMetrics, List<PaymentNotificationEntity> payments,
                                                    AdminAnalyticsRequest request) {
        // Este método ya no se usa en el flujo reactivo
        throw new UnsupportedOperationException("Use buildResponseReactive instead");
    }
    
    /**
     * Método reactivo para construir la respuesta sin bloqueo
     */
    private Uni<AnalyticsSummaryResponse> buildResponseReactive(List<PaymentNotificationEntity> payments,
                                                              AdminAnalyticsRequest request,
                                                               List<org.sky.model.SellerEntity> sellers) {
        // Calcular métricas específicas usando los calculadores
        var overview = overviewCalculator.calculateOverviewMetrics(payments, request.startDate(), request.endDate());
        var dailySales = salesCalculator.calculateDailySalesData(payments, request.startDate(), request.endDate());
        var hourlySales = salesCalculator.calculateHourlySalesData(payments, request.startDate(), request.endDate());
        var weeklySales = salesCalculator.calculateWeeklySalesData(payments, request.startDate(), request.endDate());
        var monthlySales = salesCalculator.calculateMonthlySalesData(payments, request.startDate(), request.endDate());
        var performanceMetrics = performanceCalculator.calculatePerformanceMetrics(payments);
        
        // Calcular métricas adicionales usando los calculadores
        var sellerGoals = performanceCalculator.calculateGoals(payments, request.startDate(), request.endDate());
        var sellerPerformance = performanceCalculator.calculateSellerPerformance(payments, sellers, request.startDate(), request.endDate());
        var topSellers = sellersCalculator.calculateTopSellers(payments, sellers);
        
        // Calcular métricas avanzadas usando los calculadores adicionales
        var sellerComparisons = sellersCalculator.calculateComparisons(payments, request.startDate(), request.endDate());
        var sellerTrends = sellersCalculator.calculateTrends(payments, request.startDate(), request.endDate());
        var sellerAchievements = sellersCalculator.calculateAchievements(payments, sellers, request.startDate(), request.endDate());
        var sellerInsights = insightsCalculator.calculateInsights(payments, sellers, request.startDate(), request.endDate());
        var sellerForecasting = insightsCalculator.calculateForecasting(payments, request.startDate(), request.endDate());
        var sellerAnalytics = insightsCalculator.calculateAnalytics(payments, sellers, request.startDate(), request.endDate());
        
        // Calcular métricas avanzadas de management usando Uni
        return managementCalculator.calculateBranchAnalytics(payments, sellers, request.startDate(), request.endDate())
                .chain(branchAnalytics -> 
                    managementCalculator.calculateSellerManagement(payments, sellers, request.startDate(), request.endDate())
                            .chain(sellerManagement ->
                                managementCalculator.calculateSystemMetrics(payments, sellers, request.startDate(), request.endDate())
                                        .chain(systemMetrics ->
                                            managementCalculator.calculateAdministrativeInsights(payments, sellers, request.startDate(), request.endDate())
                                                    .chain(administrativeInsights ->
                                                        managementCalculator.calculateFinancialOverview(payments, sellers, request.startDate(), request.endDate())
                                                                .chain(financialOverview ->
                                                                    managementCalculator.calculateComplianceAndSecurity(payments, sellers, request.startDate(), request.endDate())
                                                                            .map(complianceAndSecurity -> new AnalyticsSummaryResponse(
                                                                                overview, // overview
                                                                                dailySales, // dailySales
                                                                                hourlySales, // hourlySales
                                                                                weeklySales, // weeklySales
                                                                                monthlySales, // monthlySales
                                                                                topSellers, // topSellers
                                                                                performanceMetrics, // performanceMetrics
                                                                                sellerGoals, // sellerGoals
                                                                                sellerPerformance, // sellerPerformance
                                                                                sellerComparisons, // sellerComparisons
                                                                                sellerTrends, // sellerTrends
                                                                                sellerAchievements, // sellerAchievements
                                                                                sellerInsights, // sellerInsights
                                                                                sellerForecasting, // sellerForecasting
                                                                                sellerAnalytics, // sellerAnalytics
                                                                                new BranchAnalytics(branchAnalytics.branchPerformance(), branchAnalytics.branchComparison()), // branchAnalytics
                                                                                new SellerManagement(sellerManagement.sellerOverview(), sellerManagement.sellerPerformanceDistribution(), sellerManagement.sellerActivity()), // sellerManagement
                                                                                new SystemMetrics(systemMetrics.overallSystemHealth(), systemMetrics.paymentSystemMetrics(), systemMetrics.userEngagement()), // systemMetrics
                                                                                new AdministrativeInsights(administrativeInsights.managementAlerts(), administrativeInsights.recommendations(), administrativeInsights.growthOpportunities()), // administrativeInsights
                                                                                new FinancialOverview(financialOverview.revenueBreakdown(), financialOverview.costAnalysis()), // financialOverview
                                                                                new ComplianceAndSecurity(complianceAndSecurity.securityMetrics(), complianceAndSecurity.complianceStatus()) // complianceAndSecurity
                                                                            ))
                                                                )
                                                    )
                                        )
                            )
                );
    }
    
    /**
     * Métricas específicas para admin analytics
     */
    private record AdminAnalyticsSpecificMetrics(
        OverviewMetrics overview,
        List<DailySalesData> dailySales,
        List<HourlySalesData> hourlySales,
        List<WeeklySalesData> weeklySales,
        List<MonthlySalesData> monthlySales,
        PerformanceMetrics performance
    ) {}
}
