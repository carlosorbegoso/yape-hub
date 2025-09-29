package org.sky.service.stats.services;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.stats.AnalyticsSummaryResponse;
import org.sky.dto.stats.SellerAnalyticsResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.service.stats.analytics.AdminAnalyticsProcessor;
import org.sky.service.stats.analytics.SellerAnalyticsProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AnalyticsDataProcessor {
    
    @Inject
    AdminAnalyticsProcessor adminAnalyticsProcessor;
    
    @Inject
    SellerAnalyticsProcessor sellerAnalyticsProcessor;
    
    private static final Logger log = Logger.getLogger(AnalyticsDataProcessor.class);
    
    public AnalyticsSummaryResponse processAdminAnalytics(List<PaymentNotification> payments, List<Seller> sellers, LocalDate startDate, LocalDate endDate) {
        
        // Calcular métricas de resumen
        AnalyticsSummaryResponse.OverviewMetrics overview = adminAnalyticsProcessor.calculateOverviewMetrics(payments, startDate, endDate);
        
        // Calcular ventas diarias
        List<AnalyticsSummaryResponse.DailySalesData> dailySales = adminAnalyticsProcessor.calculateDailySalesData(payments, startDate, endDate);
        
        // Calcular top vendedores
        List<AnalyticsSummaryResponse.TopSellerData> topSellers = adminAnalyticsProcessor.calculateTopSellers(payments, sellers);
        
        // Calcular métricas de rendimiento
        AnalyticsSummaryResponse.PerformanceMetrics performance = adminAnalyticsProcessor.calculatePerformanceMetrics(payments);
        
        // Calcular datos avanzados para admin analytics
        List<AnalyticsSummaryResponse.HourlySalesData> hourlySales = adminAnalyticsProcessor.calculateHourlySalesData(payments, startDate, endDate);
        List<AnalyticsSummaryResponse.WeeklySalesData> weeklySales = adminAnalyticsProcessor.calculateWeeklySalesData(payments, startDate, endDate);
        List<AnalyticsSummaryResponse.MonthlySalesData> monthlySales = adminAnalyticsProcessor.calculateMonthlySalesData(payments, startDate, endDate);
        
        // Calcular métricas avanzadas para admin
        AnalyticsSummaryResponse.SellerGoals goals = adminAnalyticsProcessor.calculateGoals(payments, startDate, endDate);
        AnalyticsSummaryResponse.SellerPerformance sellerPerformance = adminAnalyticsProcessor.calculateSellerPerformance(payments, sellers, startDate, endDate);
        AnalyticsSummaryResponse.SellerComparisons comparisons = adminAnalyticsProcessor.calculateComparisons(payments, startDate, endDate);
        AnalyticsSummaryResponse.SellerTrends trends = adminAnalyticsProcessor.calculateTrends(payments, startDate, endDate);
        AnalyticsSummaryResponse.SellerAchievements achievements = adminAnalyticsProcessor.calculateAchievements(payments, sellers, startDate, endDate);
        AnalyticsSummaryResponse.SellerInsights insights = adminAnalyticsProcessor.calculateInsights(payments, sellers, startDate, endDate);
        AnalyticsSummaryResponse.SellerForecasting forecasting = adminAnalyticsProcessor.calculateForecasting(payments, startDate, endDate);
        AnalyticsSummaryResponse.SellerAnalytics analytics = adminAnalyticsProcessor.calculateAnalytics(payments, sellers, startDate, endDate);
        
        // Calcular nuevos campos avanzados
        AnalyticsSummaryResponse.BranchAnalytics branchAnalytics = adminAnalyticsProcessor.calculateBranchAnalytics(payments, sellers, startDate, endDate);
        AnalyticsSummaryResponse.SellerManagement sellerManagement = adminAnalyticsProcessor.calculateSellerManagement(payments, sellers, startDate, endDate);
        AnalyticsSummaryResponse.SystemMetrics systemMetrics = adminAnalyticsProcessor.calculateSystemMetrics(payments, sellers, startDate, endDate);
        AnalyticsSummaryResponse.AdministrativeInsights administrativeInsights = adminAnalyticsProcessor.calculateAdministrativeInsights(payments, sellers, startDate, endDate);
        AnalyticsSummaryResponse.FinancialOverview financialOverview = adminAnalyticsProcessor.calculateFinancialOverview(payments, sellers, startDate, endDate);
        AnalyticsSummaryResponse.ComplianceAndSecurity complianceAndSecurity = adminAnalyticsProcessor.calculateComplianceAndSecurity(payments, sellers, startDate, endDate);
        
                                return new AnalyticsSummaryResponse(
            overview, dailySales, hourlySales, weeklySales, monthlySales, 
            topSellers, performance, goals, sellerPerformance, comparisons, 
            trends, achievements, insights, forecasting, analytics,
            branchAnalytics, sellerManagement, systemMetrics, 
            administrativeInsights, financialOverview, complianceAndSecurity
        );
    }
    
    public SellerAnalyticsResponse processSellerAnalytics(List<PaymentNotification> payments, Long sellerId, LocalDate startDate, LocalDate endDate) {
        
        // Filtrar solo los pagos relacionados con este vendedor
        List<PaymentNotification> sellerPayments = payments.stream()
                .filter(p -> sellerId.equals(p.confirmedBy) || sellerId.equals(p.rejectedBy))
                .collect(Collectors.toList());
        
        log.info("📊 Pagos filtrados para seller " + sellerId + ": " + sellerPayments.size() + " pagos");
        log.info("📊 Pagos confirmados por seller: " + sellerPayments.stream().filter(p -> sellerId.equals(p.confirmedBy)).count());
        log.info("📊 Pagos rechazados por seller: " + sellerPayments.stream().filter(p -> sellerId.equals(p.rejectedBy)).count());
        
        // Calcular métricas de resumen específicas del vendedor
        SellerAnalyticsResponse.OverviewMetrics overview = sellerAnalyticsProcessor.calculateOverviewMetrics(sellerPayments, startDate, endDate);
        
        // Calcular ventas diarias del vendedor
        List<SellerAnalyticsResponse.DailySalesData> dailySales = sellerAnalyticsProcessor.calculateDailySalesData(sellerPayments, startDate, endDate);
        
        // Calcular métricas de rendimiento del vendedor
        SellerAnalyticsResponse.PerformanceMetrics performance = sellerAnalyticsProcessor.calculatePerformanceMetrics(sellerPayments);
        
        // Calcular datos adicionales para analytics avanzados
        List<SellerAnalyticsResponse.HourlySalesData> hourlySales = sellerAnalyticsProcessor.calculateHourlySalesData(sellerPayments, startDate, endDate);
        List<SellerAnalyticsResponse.WeeklySalesData> weeklySales = sellerAnalyticsProcessor.calculateWeeklySalesData(sellerPayments, startDate, endDate);
        List<SellerAnalyticsResponse.MonthlySalesData> monthlySales = sellerAnalyticsProcessor.calculateMonthlySalesData(sellerPayments, startDate, endDate);
        
        // Calcular métricas avanzadas
        SellerAnalyticsResponse.SellerGoals goals = sellerAnalyticsProcessor.calculateGoals(sellerPayments, startDate, endDate);
        SellerAnalyticsResponse.SellerPerformance sellerPerformance = sellerAnalyticsProcessor.calculatePerformance(sellerPayments, startDate, endDate);
        SellerAnalyticsResponse.SellerComparisons comparisons = sellerAnalyticsProcessor.calculateComparisons(sellerPayments, startDate, endDate);
        SellerAnalyticsResponse.SellerTrends trends = sellerAnalyticsProcessor.calculateTrends(sellerPayments, startDate, endDate);
        SellerAnalyticsResponse.SellerAchievements achievements = sellerAnalyticsProcessor.calculateAchievements(sellerPayments, startDate, endDate);
        SellerAnalyticsResponse.SellerInsights insights = sellerAnalyticsProcessor.calculateInsights(sellerPayments, startDate, endDate);
        SellerAnalyticsResponse.SellerForecasting forecasting = sellerAnalyticsProcessor.calculateForecasting(sellerPayments, startDate, endDate);
        SellerAnalyticsResponse.SellerAnalytics analytics = sellerAnalyticsProcessor.calculateAnalytics(sellerPayments, startDate, endDate);
        
        return new SellerAnalyticsResponse(
            overview, dailySales, hourlySales, weeklySales, monthlySales, 
            performance, goals, sellerPerformance, comparisons, 
            trends, achievements, insights, forecasting, analytics
        );
    }
}
