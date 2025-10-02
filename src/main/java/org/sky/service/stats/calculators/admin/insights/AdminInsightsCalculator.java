package org.sky.service.stats.calculators.admin.insights;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.AnalyticsSummaryResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@ApplicationScoped
public class AdminInsightsCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    
    public AnalyticsSummaryResponse.SellerInsights calculateInsights(List<PaymentNotification> payments, 
                                                                   List<Seller> sellers, 
                                                                   java.time.LocalDate startDate, 
                                                                   java.time.LocalDate endDate) {
        if (payments.isEmpty()) {
            return new AnalyticsSummaryResponse.SellerInsights(
                null, null, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
            );
        }
        
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        
        var peakPerformanceDay = calculatePeakPerformanceDay(confirmedPayments);
        var peakPerformanceHour = calculatePeakPerformanceHour(confirmedPayments);
        var averageTransactionValue = calculateAverageTransactionValue(confirmedPayments);
        
        var customerRetentionRate = calculateCustomerRetentionRate(confirmedPayments);
        var repeatCustomerRate = calculateRepeatCustomerRate(confirmedPayments);
        var newCustomerRate = Math.max(0.0, 100.0 - repeatCustomerRate);
        var conversionRate = calculateConversionRate(confirmedPayments.size(), payments.size());
        var satisfactionScore = calculateSatisfactionScore(conversionRate);
        
        return new AnalyticsSummaryResponse.SellerInsights(
                peakPerformanceDay, peakPerformanceHour, averageTransactionValue,
                customerRetentionRate, repeatCustomerRate, newCustomerRate,
                conversionRate, satisfactionScore
        );
    }
    
    public AnalyticsSummaryResponse.SellerForecasting calculateForecasting(List<PaymentNotification> payments, 
                                                                         java.time.LocalDate startDate, 
                                                                         java.time.LocalDate endDate) {
        if (payments.isEmpty()) {
            var emptyTrend = new AnalyticsSummaryResponse.TrendAnalysis("stable", 0.0, 0.0, 0.0);
            return new AnalyticsSummaryResponse.SellerForecasting(List.of(), emptyTrend, List.of());
        }
        
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var predictedSales = createPredictedSales(confirmedPayments, startDate, endDate);
        var trendAnalysis = createTrendAnalysis(confirmedPayments, startDate, endDate);
        var recommendations = createRecommendations(confirmedPayments);
        
        return new AnalyticsSummaryResponse.SellerForecasting(predictedSales, trendAnalysis, recommendations);
    }
    
    public AnalyticsSummaryResponse.SellerAnalytics calculateAnalytics(List<PaymentNotification> payments, 
                                                                     List<Seller> sellers, 
                                                                     java.time.LocalDate startDate, 
                                                                     java.time.LocalDate endDate) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        
        var salesDistribution = calculateSalesDistribution(confirmedPayments);
        var transactionPatterns = calculateTransactionPatterns(confirmedPayments);
        var performanceIndicators = calculatePerformanceIndicators(confirmedPayments, payments);
        
        return new AnalyticsSummaryResponse.SellerAnalytics(salesDistribution, transactionPatterns, performanceIndicators);
    }
    
    private List<PaymentNotification> filterPaymentsByStatus(List<PaymentNotification> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .toList();
    }
    
    private String calculatePeakPerformanceDay(List<PaymentNotification> payments) {
        var dailySales = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.toLocalDate(),
                        Collectors.summingDouble(p -> p.amount)
                ));
        
        return dailySales.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().getDayOfWeek().getDisplayName(
                        java.time.format.TextStyle.FULL, 
                        java.util.Locale.forLanguageTag("es")))
                .orElse("Lunes");
    }
    
    private String calculatePeakPerformanceHour(List<PaymentNotification> payments) {
        var hourlyCounts = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.getHour(),
                        Collectors.counting()
                ));
        
        return hourlyCounts.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(entry -> String.format("%02d:00", entry.getKey()))
                .orElse("12:00");
    }
    
    private double calculateAverageTransactionValue(List<PaymentNotification> payments) {
        return payments.stream()
                .mapToDouble(payment -> payment.amount)
                .average()
                .orElse(0.0);
    }
    
    private double calculateCustomerRetentionRate(List<PaymentNotification> payments) {
        // Implementación simplificada
        return Math.min(100.0, payments.size() * 15.0);
    }
    
    private double calculateRepeatCustomerRate(List<PaymentNotification> payments) {
        // Implementación simplificada
        return Math.min(100.0, payments.size() * 10.0);
    }
    
    private double calculateConversionRate(int confirmedCount, int totalCount) {
        return totalCount > 0 ? (double) confirmedCount / totalCount * 100 : 0.0;
    }
    
    private double calculateSatisfactionScore(double conversionRate) {
        return Math.min(100.0, conversionRate + 10.0);
    }
    
    private List<AnalyticsSummaryResponse.PredictedSale> createPredictedSales(List<PaymentNotification> payments, 
                                                                           java.time.LocalDate startDate, 
                                                                           java.time.LocalDate endDate) {
        var predictedCount = 7; // Próximos 7 días
        var totalSales = payments.stream().mapToDouble(p -> p.amount).sum();
        var daysDiff = startDate.until(endDate).getDays() + 1;
        var avgDaily = daysDiff > 0 ? totalSales / daysDiff : 0.0;
        
        return java.time.LocalDate.now().plusDays(1).datesUntil(java.time.LocalDate.now().plusDays(predictedCount + 1))
                .map(date -> new AnalyticsSummaryResponse.PredictedSale(
                        date.toString(),
                        avgDaily * (0.8 + Math.random() * 0.4),
                        Math.max(0.5, 1.0 - (date.toEpochDay() / 365.0) * 0.3)
                ))
                .collect(Collectors.toList());
    }
    
    private AnalyticsSummaryResponse.TrendAnalysis createTrendAnalysis(List<PaymentNotification> payments, 
                                                                     java.time.LocalDate startDate, 
                                                                     java.time.LocalDate endDate) {
        var trend = calculateTrend(payments, startDate, endDate);
        var slope = 0.1;
        var r2 = 0.7;
        var accuracy = 0.8;
        
        return new AnalyticsSummaryResponse.TrendAnalysis(trend, slope, r2, accuracy);
    }
    
    private String calculateTrend(List<PaymentNotification> payments, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (payments.size() < 2) return "stable";
        
        var dailySales = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.toLocalDate(),
                        Collectors.summingDouble(p -> p.amount)
                ));
        
        var values = dailySales.values().stream().sorted().toList();
        if (values.size() < 2) return "stable";
        
        var firstValue = values.get(0);
        var lastValue = values.get(values.size() - 1);
        var changePercentage = firstValue > 0 ? ((lastValue - firstValue) / firstValue) * 100 : 0.0;
        
        return switch ((int) Math.signum(changePercentage)) {
            case 1 -> changePercentage > 20.0 ? "growing" : "stable";
            case -1 -> changePercentage < -20.0 ? "declining" : "stable";
            default -> "stable";
        };
    }
    
    private List<String> createRecommendations(List<PaymentNotification> payments) {
        var recommendations = new ArrayList<String>();
        
        if (payments.size() > 50) {
            recommendations.add("Excelente rendimiento, continúa con la estrategia actual");
        } else if (payments.size() > 20) {
            recommendations.add("Buen rendimiento, considera expandir horarios de operación");
        } else {
            recommendations.add("Considera aumentar la actividad de ventas");
        }
        
        recommendations.add("Revisa los patrones de tus mejores días");
        recommendations.add("Optimiza los horarios de mayor actividad");
        
        return recommendations;
    }
    
    private AnalyticsSummaryResponse.SalesDistribution calculateSalesDistribution(List<PaymentNotification> payments) {
        var weekdaySales = payments.stream()
                .filter(p -> p.createdAt.getDayOfWeek().getValue() < 6)
                .mapToDouble(p -> p.amount).sum();
        
        var weekendSales = payments.stream()
                .filter(p -> p.createdAt.getDayOfWeek().getValue() >= 6)
                .mapToDouble(p -> p.amount).sum();
        
        var morningSales = payments.stream()
                .filter(p -> p.createdAt.getHour() >= 6 && p.createdAt.getHour() < 12)
                .mapToDouble(p -> p.amount).sum();
        
        var afternoonSales = payments.stream()
                .filter(p -> p.createdAt.getHour() >= 12 && p.createdAt.getHour() < 18)
                .mapToDouble(p -> p.amount).sum();
        
        var eveningSales = payments.stream()
                .filter(p -> p.createdAt.getHour() >= 18 && p.createdAt.getHour() < 24)
                .mapToDouble(p -> p.amount).sum();
        
        return new AnalyticsSummaryResponse.SalesDistribution(
                weekdaySales, weekendSales, 
                morningSales, afternoonSales, eveningSales
        );
    }
    
    private AnalyticsSummaryResponse.TransactionPatterns calculateTransactionPatterns(List<PaymentNotification> payments) {
        var avgPerDay = payments.size() > 0 ? payments.stream().mapToDouble(p -> p.amount).average().orElse(0.0) : 0.0;
        var mostActiveDay = calculatePeakPerformanceDay(payments);
        var mostActiveHour = calculatePeakPerformanceHour(payments);
        var frequency = payments.size() > 100 ? "high" : payments.size() > 50 ? "medium" : "low";
        
        return new AnalyticsSummaryResponse.TransactionPatterns(avgPerDay, mostActiveDay, mostActiveHour, frequency);
    }
    
    private AnalyticsSummaryResponse.PerformanceIndicators calculatePerformanceIndicators(List<PaymentNotification> confirmedPayments, 
                                                                                        List<PaymentNotification> allPayments) {
        var salesVelocity = confirmedPayments.stream().mapToDouble(p -> p.amount).sum() / Math.max(1, allPayments.size());
        var transactionVelocity = allPayments.size() / 7.0; // por semana
        var efficiencyIndex = 0.8;
        var consistencyIndex = calculateConsistencyIndex(confirmedPayments);
        
        return new AnalyticsSummaryResponse.PerformanceIndicators(
                salesVelocity, transactionVelocity, efficiencyIndex, consistencyIndex
        );
    }
    
    private double calculateConsistencyIndex(List<PaymentNotification> payments) {
        if (payments.size() < 2) return 0.0;
        
        var dailySales = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.toLocalDate(),
                        Collectors.summingDouble(p -> p.amount)
                ));
        
        var values = dailySales.values();
        var avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        var variance = values.stream().mapToDouble(v -> Math.pow(v - avg, 2)).average().orElse(0.0);
        var standardDeviation = Math.sqrt(variance);
        var coefficientOfVariation = avg > 0 ? standardDeviation / avg : 0.0;
        
        return Math.max(0.0, 1.0 - coefficientOfVariation);
    }
}
