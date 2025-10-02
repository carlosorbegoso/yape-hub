package org.sky.service.stats.calculators.seller.analytics;

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@ApplicationScoped
public class SellerAnalyticsDataCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    
    public SellerAnalytics calculateSellerAnalytics(List<PaymentNotification> sellerPayments, 
                                                                            List<PaymentNotification> allPayments, 
                                                                            SellerAnalyticsRequest request) {
        // Calcular analytics usando granularity y metric
        var salesDistribution = calculateSalesDistribution(sellerPayments, request.granularity());
        var transactionPatterns = calculateTransactionPatterns(sellerPayments, request.metric(), request.granularity());
        var performanceIndicators = calculatePerformanceIndicators(sellerPayments, request.metric(), request.confidence());
        
        return new SellerAnalytics(salesDistribution, transactionPatterns, performanceIndicators);
    }
    
    private SalesDistribution calculateSalesDistribution(List<PaymentNotification> payments, String granularity) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        
        // Calcular distribución por días de la semana
        var weekdaySales = confirmedPayments.stream()
                .filter(p -> p.createdAt.getDayOfWeek().getValue() < 6)
                .mapToDouble(p -> p.amount).sum();
        
        var weekendSales = confirmedPayments.stream()
                .filter(p -> p.createdAt.getDayOfWeek().getValue() >= 6)
                .mapToDouble(p -> p.amount).sum();
        
        // Calcular distribución por períodos del día
        var morningSales = confirmedPayments.stream()
                .filter(p -> p.createdAt.getHour() >= 6 && p.createdAt.getHour() < 12)
                .mapToDouble(p -> p.amount).sum();
        
        var afternoonSales = confirmedPayments.stream()
                .filter(p -> p.createdAt.getHour() >= 12 && p.createdAt.getHour() < 18)
                .mapToDouble(p -> p.amount).sum();
        
        var eveningSales = confirmedPayments.stream()
                .filter(p -> p.createdAt.getHour() >= 18 && p.createdAt.getHour() < 24)
                .mapToDouble(p -> p.amount).sum();
        
        return new SalesDistribution(
                weekdaySales, weekendSales, 
                morningSales, afternoonSales, eveningSales
        );
    }
    
    private TransactionPatterns calculateTransactionPatterns(List<PaymentNotification> payments, 
                                                                                   String metric, String granularity) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var totalSales = calculateTotalSales(confirmedPayments);
        var totalTransactions = payments.size();
        
        var avgPerDay = totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
        
        // Encontrar día más activo
        var mostActiveDay = findMostActiveDay(confirmedPayments);
        
        // Encontrar hora más activa
        var mostActiveHour = findMostActiveHour(confirmedPayments, granularity);
        
        // Determinar frecuencia basada en métricas
        var frequency = determineFrequency(totalTransactions, metric);
        
        return new TransactionPatterns(avgPerDay, mostActiveDay, mostActiveHour, frequency);
    }
    
    private PerformanceIndicators calculatePerformanceIndicators(List<PaymentNotification> payments, 
                                                                                        String metric, Double confidence) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var totalSales = calculateTotalSales(confirmedPayments);
        var totalTransactions = payments.size();
        
        // Calcular velocidad de ventas
        var salesVelocity = totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
        
        // Calcular velocidad de transacciones (por semana)
        var transactionVelocity = totalTransactions / 7.0;
        
        // Calcular índice de eficiencia basado en confidence
        var efficiencyIndex = confidence != null ? confidence : 0.8;
        
        // Calcular índice de consistencia
        var consistencyIndex = calculateConsistencyIndex(confirmedPayments, confidence);
        
        return new PerformanceIndicators(
                salesVelocity, transactionVelocity, efficiencyIndex, consistencyIndex
        );
    }
    
    private String findMostActiveDay(List<PaymentNotification> confirmedPayments) {
        if (confirmedPayments.isEmpty()) return "2024-01-01";
        
        var dailySales = confirmedPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.toLocalDate().toString(),
                        Collectors.summingDouble(p -> p.amount)
                ));
        
        return dailySales.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("2024-01-01");
    }
    
    private String findMostActiveHour(List<PaymentNotification> confirmedPayments, String granularity) {
        if (confirmedPayments.isEmpty()) return "12:00";
        
        var hourlyCounts = confirmedPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.getHour(),
                        Collectors.counting()
                ));
        
        var mostActiveHour = hourlyCounts.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(12);
        
        return String.format("%02d:00", mostActiveHour);
    }
    
    private String determineFrequency(int totalTransactions, String metric) {
        return switch (metric != null ? metric.toLowerCase() : "default") {
            case "high" -> totalTransactions > 100 ? "high" : totalTransactions > 50 ? "medium" : "low";
            case "medium" -> totalTransactions > 50 ? "medium" : "low";
            case "low" -> "low";
            default -> totalTransactions > 100 ? "high" : totalTransactions > 20 ? "medium" : "low";
        };
    }
    
    private double calculateConsistencyIndex(List<PaymentNotification> confirmedPayments, Double confidence) {
        if (confirmedPayments.size() < 2) return 0.0;
        
        // Calcular consistencia basada en la variación de ventas diarias
        var dailySales = confirmedPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.toLocalDate(),
                        Collectors.summingDouble(p -> p.amount)
                ));
        
        var salesList = new ArrayList<>(dailySales.values());
        
        if (salesList.size() < 2) return 0.0;
        
        var mean = salesList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        var variance = salesList.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        
        var standardDeviation = Math.sqrt(variance);
        var coefficientOfVariation = mean > 0 ? standardDeviation / mean : 0.0;
        
        // Convertir a índice de consistencia (0-1)
        var consistencyIndex = Math.max(0.0, 1.0 - coefficientOfVariation);
        
        // Ajustar con confidence
        var confidenceFactor = confidence != null ? confidence : 1.0;
        return Math.min(1.0, consistencyIndex * confidenceFactor);
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
}
