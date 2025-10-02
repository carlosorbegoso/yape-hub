package org.sky.service.stats.calculators.seller.performance;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.SellerAnalyticsRequest;
import org.sky.dto.stats.SellerAnalyticsResponse;
import org.sky.model.PaymentNotification;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@ApplicationScoped
public class SellerPerformanceCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String PENDING_STATUS = "PENDING";
    private static final String REJECTED_BY_SELLER_STATUS = "REJECTED_BY_SELLER";
    
    public SellerAnalyticsResponse.PerformanceMetrics calculatePerformanceMetrics(List<PaymentNotification> sellerPayments, 
                                                                                 SellerAnalyticsRequest request) {
        var confirmedPayments = filterPaymentsByStatus(sellerPayments, CONFIRMED_STATUS);
        var pendingPayments = filterPaymentsByStatus(sellerPayments, PENDING_STATUS);
        var rejectedPayments = filterPaymentsByStatus(sellerPayments, REJECTED_BY_SELLER_STATUS);
        
        var claimRate = calculateClaimRate(confirmedPayments.size(), sellerPayments.size());
        var rejectionRate = calculateRejectionRate(rejectedPayments.size(), sellerPayments.size());
        var averageConfirmationTime = calculateAverageConfirmationTime(confirmedPayments);
        
        return new SellerAnalyticsResponse.PerformanceMetrics(
                averageConfirmationTime, claimRate, rejectionRate,
                (long) pendingPayments.size(), (long) confirmedPayments.size(), (long) rejectedPayments.size()
        );
    }
    
    public SellerAnalyticsResponse.SellerPerformance calculateSellerPerformance(List<PaymentNotification> sellerPayments, 
                                                                               List<PaymentNotification> allPayments, 
                                                                               SellerAnalyticsRequest request) {
        if (sellerPayments.isEmpty()) {
            return new SellerAnalyticsResponse.SellerPerformance(
                null, null, 0.0, 0.0, List.of(), 0.0, 0.0, 0.0
            );
        }
        
        // Calcular días de mejor/peor rendimiento usando metric
        var dailySalesMap = calculateDailySalesMap(sellerPayments, request.startDate(), request.endDate());
        var bestDay = findBestDay(dailySalesMap, request.metric());
        var worstDay = findWorstDay(dailySalesMap, request.metric());
        
        // Calcular promedio diario considerando el período
        var daysInPeriod = calculateDaysInPeriod(request.startDate(), request.endDate(), request.period());
        var totalSales = calculateTotalSales(filterPaymentsByStatus(sellerPayments, CONFIRMED_STATUS));
        var averageDailySales = totalSales / daysInPeriod;
        
        // Calcular horas pico usando granularity
        var peakHours = calculatePeakHours(sellerPayments, request.granularity());
        
        // Métricas de rendimiento usando confidence para ajustar cálculos
        var consistencyScore = calculateConsistencyScore(dailySalesMap.values(), request.confidence());
        var productivityScore = calculateProductivityScore(averageDailySales, request.metric(), request.confidence());
        var efficiencyRate = calculateEfficiencyRate(sellerPayments, request.confidence());
        var responseTime = calculateAverageConfirmationTime(filterPaymentsByStatus(sellerPayments, CONFIRMED_STATUS));
        
        return new SellerAnalyticsResponse.SellerPerformance(
                bestDay, worstDay, averageDailySales, consistencyScore,
                peakHours, productivityScore, efficiencyRate, responseTime
        );
    }
    
    private Map<String, Double> calculateDailySalesMap(List<PaymentNotification> payments, 
                                                      java.time.LocalDate startDate, 
                                                      java.time.LocalDate endDate) {
        return payments.stream()
                .filter(p -> CONFIRMED_STATUS.equals(p.status))
                .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.toLocalDate().toString(),
                        Collectors.summingDouble(p -> p.amount)
                ));
    }
    
    private String findBestDay(Map<String, Double> dailySales, String metric) {
        return dailySales.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    private String findWorstDay(Map<String, Double> dailySales, String metric) {
        return dailySales.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
    
    private int calculateDaysInPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate, String period) {
        var daysDiff = startDate.until(endDate).getDays() + 1;
        return switch (period != null ? period.toLowerCase() : "daily") {
            case "weekly" -> Math.max(1, daysDiff / 7);
            case "monthly" -> Math.max(1, daysDiff / 30);
            case "yearly" -> Math.max(1, daysDiff / 365);
            default -> daysDiff;
        };
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
    
    private List<String> calculatePeakHours(List<PaymentNotification> payments, String granularity) {
        var hourlySales = payments.stream()
                .filter(p -> CONFIRMED_STATUS.equals(p.status))
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.getHour(),
                        Collectors.summingDouble(p -> p.amount)
                ));
        
        return hourlySales.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(granularity != null && granularity.equals("hourly") ? 5 : 2)
                .map(entry -> String.format("%02d:00", entry.getKey()))
                .collect(Collectors.toList());
    }
    
    private double calculateConsistencyScore(java.util.Collection<Double> dailyValues, Double confidence) {
        if (dailyValues.size() < 2) return 0.0;
        var avg = dailyValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        var confidenceFactor = confidence != null ? confidence : 1.0;
        return Math.min(100.0, avg > 0 ? 100.0 - (calculateStandardDeviation(dailyValues) / avg) * confidenceFactor * 100 : 0.0);
    }
    
    private double calculateStandardDeviation(java.util.Collection<Double> values) {
        var avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        var variance = values.stream().mapToDouble(v -> Math.pow(v - avg, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }
    
    private double calculateProductivityScore(double averageDailySales, String metric, Double confidence) {
        var baseScore = Math.min(averageDailySales / 50.0, 1.0) * 100;
        var confidenceFactor = confidence != null ? confidence : 1.0;
        return baseScore * confidenceFactor;
    }
    
    private double calculateEfficiencyRate(List<PaymentNotification> payments, Double confidence) {
        var confirmedCount = filterPaymentsByStatus(payments, CONFIRMED_STATUS).size();
        var totalCount = payments.size();
        var baseRate = totalCount > 0 ? (double) confirmedCount / totalCount * 100 : 0.0;
        var confidenceFactor = confidence != null ? confidence : 1.0;
        return Math.min(baseRate * confidenceFactor, 100.0);
    }
    
    private double calculateAverageConfirmationTime(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
                .filter(payment -> payment.confirmedAt != null)
                .mapToDouble(payment -> java.time.Duration.between(payment.createdAt, payment.confirmedAt).toMinutes())
                .average()
                .orElse(0.0);
    }
    
    private double calculateClaimRate(int confirmedCount, int totalCount) {
        return totalCount > 0 ? (double) confirmedCount / totalCount * 100 : 0.0;
    }
    
    private double calculateRejectionRate(int rejectedCount, int totalCount) {
        return totalCount > 0 ? (double) rejectedCount / totalCount * 100 : 0.0;
    }
}