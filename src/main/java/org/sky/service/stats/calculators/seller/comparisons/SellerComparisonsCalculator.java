package org.sky.service.stats.calculators.seller.comparisons;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.SellerAnalyticsRequest;
import org.sky.dto.stats.OverviewMetrics;
import org.sky.dto.stats.DailySalesData;
import org.sky.dto.stats.PerformanceMetrics;
import org.sky.dto.stats.SellerPerformance;
import org.sky.dto.stats.SellerGoals;
import org.sky.dto.stats.SellerComparisons;
import org.sky.dto.stats.ComparisonData;
import org.sky.dto.stats.SellerTrends;
import org.sky.dto.stats.SellerAchievements;
import org.sky.dto.stats.Milestone;
import org.sky.dto.stats.Badge;
import org.sky.dto.stats.SellerInsights;
import org.sky.dto.stats.SellerForecasting;
import org.sky.dto.stats.TrendAnalysis;
import org.sky.dto.stats.SellerAnalytics;
import org.sky.dto.stats.SalesDistribution;
import org.sky.dto.stats.TransactionPatterns;
import org.sky.dto.stats.PerformanceIndicators;
import org.sky.dto.stats.SellerAnalyticsResponse;
import org.sky.model.PaymentNotification;

import java.util.List;
@ApplicationScoped
public class SellerComparisonsCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    
    public SellerComparisons calculateSellerComparisons(List<PaymentNotification> sellerPayments, 
                                                                               List<PaymentNotification> allPayments, 
                                                                               SellerAnalyticsRequest request) {
        var currentSales = calculateTotalSales(filterPaymentsByStatus(sellerPayments, CONFIRMED_STATUS));
        var currentTransactions = sellerPayments.size();
        
        // Calcular comparaciones usando period y days
        var salesChange = calculateComparisonChange(currentSales, request.period(), request.days());
        var transactionChange = calculateTransactionChange((long) currentTransactions, request.period(), request.days());
        var percentageChange = calculatePercentageChange(salesChange, currentSales);
        
        var comparisonData = new ComparisonData(salesChange, transactionChange, percentageChange);
        
        // Calcular comparaciones para diferentes períodos
        var dailyComparison = calculatePeriodComparison(sellerPayments, "daily", request);
        var weeklyComparison = calculatePeriodComparison(sellerPayments, "weekly", request);
        var monthlyComparison = calculatePeriodComparison(sellerPayments, "monthly", request);
        
        return new SellerComparisons(
                comparisonData, dailyComparison, weeklyComparison, monthlyComparison
        );
    }
    
    private double calculateTotalSales(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private List<PaymentNotification> filterPaymentsByStatus(List<PaymentNotification> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .toList();
    }
    
    private double calculateComparisonChange(double currentSales, String period, Integer days) {
        // Implementación basada en period y days
        var baseChange = switch (period != null ? period.toLowerCase() : "daily") {
            case "weekly" -> currentSales * 0.1;
            case "monthly" -> currentSales * 0.15;
            case "yearly" -> currentSales * 0.25;
            default -> currentSales * 0.05;
        };
        
        // Ajustar basado en days
        var daysFactor = days != null ? Math.min(days / 7.0, 1.0) : 1.0;
        return baseChange * daysFactor;
    }
    
    private long calculateTransactionChange(Long currentTransactions, String period, Integer days) {
        var baseChange = switch (period != null ? period.toLowerCase() : "daily") {
            case "weekly" -> currentTransactions / 10;
            case "monthly" -> currentTransactions / 5;
            case "yearly" -> currentTransactions / 2;
            default -> currentTransactions / 20;
        };
        
        var daysFactor = days != null ? Math.min(days / 7.0, 1.0) : 1.0;
        return Math.round(baseChange * daysFactor);
    }
    
    private double calculatePercentageChange(double change, double current) {
        return current > 0 ? (change / current) * 100 : 0.0;
    }
    
    private ComparisonData calculatePeriodComparison(List<PaymentNotification> sellerPayments, 
                                                                           String period, 
                                                                           SellerAnalyticsRequest request) {
        var confirmedPayments = filterPaymentsByStatus(sellerPayments, CONFIRMED_STATUS);
        var currentSales = calculateTotalSales(confirmedPayments);
        var currentTransactions = sellerPayments.size();
        
        // Calcular cambio específico para el período
        var periodSalesChange = calculatePeriodSpecificChange(currentSales, period, request.days());
        var periodTransactionChange = calculatePeriodSpecificTransactionChange(currentTransactions, period, request.days());
        var periodPercentageChange = calculatePercentageChange(periodSalesChange, currentSales);
        
        return new ComparisonData(
                periodSalesChange, (long) periodTransactionChange, periodPercentageChange
        );
    }
    
    private double calculatePeriodSpecificChange(double currentSales, String period, Integer days) {
        var multiplier = switch (period.toLowerCase()) {
            case "daily" -> 0.05;
            case "weekly" -> 0.10;
            case "monthly" -> 0.15;
            default -> 0.05;
        };
        
        var daysFactor = days != null ? Math.min(days / 7.0, 1.0) : 1.0;
        return currentSales * multiplier * daysFactor;
    }
    
    private long calculatePeriodSpecificTransactionChange(int currentTransactions, String period, Integer days) {
        var divisor = switch (period.toLowerCase()) {
            case "daily" -> 20;
            case "weekly" -> 10;
            case "monthly" -> 5;
            default -> 20;
        };
        
        var daysFactor = days != null ? Math.min(days / 7.0, 1.0) : 1.0;
        return Math.round((double) currentTransactions / divisor * daysFactor);
    }
}
