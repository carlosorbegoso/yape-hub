package org.sky.service.stats.calculators.admin.performance;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.response.stats.PerformanceMetrics;
import org.sky.dto.response.seller.SellerGoals;
import org.sky.dto.response.seller.SellerPerformance;
import org.sky.model.PaymentNotificationEntity;

import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class AdminPerformanceCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String PENDING_STATUS = "PENDING";
    private static final String REJECTED_BY_SELLER_STATUS = "REJECTED_BY_SELLER";
    
    public PerformanceMetrics calculatePerformanceMetrics(List<PaymentNotificationEntity> payments) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var pendingPayments = filterPaymentsByStatus(payments, PENDING_STATUS);
        var rejectedPayments = filterPaymentsByStatus(payments, REJECTED_BY_SELLER_STATUS);
        
        var totalPayments = payments.size();
        var averageConfirmationTime = calculateAverageConfirmationTime(confirmedPayments);
        var claimRate = calculateClaimRate(confirmedPayments.size(), totalPayments);
        var rejectionRate = calculateRejectionRate(rejectedPayments.size(), totalPayments);
        
        return new PerformanceMetrics(
                averageConfirmationTime, claimRate, rejectionRate,
                (long) pendingPayments.size(), (long) confirmedPayments.size(), (long) rejectedPayments.size()
        );
    }
    
    public SellerGoals calculateGoals(List<PaymentNotificationEntity> payments,
                                                             java.time.LocalDate startDate, 
                                                             java.time.LocalDate endDate) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var totalSales = calculateTotalSales(confirmedPayments);
        
        var daysDiff = startDate.until(endDate).getDays() + 1;
        var dailyTarget = calculateDailyTarget(totalSales, daysDiff);
        var weeklyTarget = dailyTarget * 7;
        var monthlyTarget = dailyTarget * 30;
        var yearlyTarget = dailyTarget * 365;
        
        var achievementRate = calculateAchievementRate(totalSales, dailyTarget, daysDiff);
        var dailyProgress = calculateProgressRate(totalSales, dailyTarget, daysDiff);
        var weeklyProgress = calculateProgressRate(totalSales, weeklyTarget, Math.max(1, daysDiff / 7));
        var monthlyProgress = calculateProgressRate(totalSales, monthlyTarget, Math.max(1, daysDiff / 30));
        
        return new SellerGoals(
                dailyTarget, weeklyTarget, monthlyTarget, yearlyTarget,
                achievementRate, dailyProgress, weeklyProgress, monthlyProgress
        );
    }
    
    public SellerPerformance calculateSellerPerformance(List<PaymentNotificationEntity> payments,
                                                                               List<org.sky.model.SellerEntity> sellers, 
                                                                               java.time.LocalDate startDate, 
                                                                               java.time.LocalDate endDate) {
        if (sellers.isEmpty()) {
            return new SellerPerformance(
                null, null, 0.0, 0.0, List.of(), 0.0, 0.0, 0.0
            );
        }
        
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var dailySalesMap = calculateDailySalesMap(confirmedPayments, startDate, endDate);
        
        var bestDay = findBestDay(dailySalesMap);
        var worstDay = findWorstDay(dailySalesMap);
        var averageDailySales = calculateAverageDailySales(confirmedPayments, startDate, endDate);
        var consistencyScore = calculateConsistencyScore(dailySalesMap.values());
        var peakHours = calculatePeakHours(confirmedPayments);
        var productivityScore = calculateProductivityScore(averageDailySales);
        var efficiencyRate = calculateEfficiencyRate(confirmedPayments.size(), payments.size());
        var responseTime = calculateAverageConfirmationTime(confirmedPayments);
        
        return new SellerPerformance(
                bestDay, worstDay, averageDailySales, consistencyScore,
                peakHours, productivityScore, efficiencyRate, responseTime
        );
    }
    
    private List<PaymentNotificationEntity> filterPaymentsByStatus(List<PaymentNotificationEntity> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .toList();
    }
    
    private double calculateTotalSales(List<PaymentNotificationEntity> confirmedPayments) {
        return confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private double calculateAverageConfirmationTime(List<PaymentNotificationEntity> confirmedPayments) {
        return confirmedPayments.stream()
                .filter(payment -> payment.confirmedAt != null)
                .mapToDouble(payment -> Duration.between(payment.createdAt, payment.confirmedAt).toMinutes())
                .average()
                .orElse(0.0);
    }
    
    private double calculateClaimRate(int confirmedCount, int totalCount) {
        return totalCount > 0 ? (double) confirmedCount / totalCount * 100 : 0.0;
    }
    
    private double calculateRejectionRate(int rejectedCount, int totalCount) {
        return totalCount > 0 ? (double) rejectedCount / totalCount * 100 : 0.0;
    }
    
    private double calculateDailyTarget(double totalSales, int days) {
        return days > 0 ? Math.max(50.0, (totalSales / days) * 1.2) : 50.0;
    }
    
    private double calculateAchievementRate(double actualSales, double dailyTarget, int days) {
        var expectedSales = dailyTarget * days;
        return expectedSales > 0 ? Math.min(actualSales / expectedSales, 2.0) : 0.0;
    }
    
    private double calculateProgressRate(double actual, double target, int periodDays) {
        var expected = target * periodDays;
        return expected > 0 ? Math.min(actual / expected, 2.0) : 0.0;
    }
    
    private java.util.Map<String, Double> calculateDailySalesMap(List<PaymentNotificationEntity> payments,
                                                               java.time.LocalDate startDate, 
                                                               java.time.LocalDate endDate) {
        return payments.stream()
                .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.createdAt.toLocalDate().toString(),
                        java.util.stream.Collectors.summingDouble(p -> p.amount)
                ));
    }
    
    private String findBestDay(java.util.Map<String, Double> dailySales) {
        return dailySales.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);
    }
    
    private String findWorstDay(java.util.Map<String, Double> dailySales) {
        return dailySales.entrySet().stream()
                .min(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);
    }
    
    private double calculateAverageDailySales(List<PaymentNotificationEntity> payments,
                                            java.time.LocalDate startDate, 
                                            java.time.LocalDate endDate) {
        var daysDiff = startDate.until(endDate).getDays() + 1;
        var totalSales = calculateTotalSales(payments);
        return daysDiff > 0 ? totalSales / daysDiff : 0.0;
    }
    
    private double calculateConsistencyScore(java.util.Collection<Double> dailyValues) {
        if (dailyValues.size() < 2) return 0.0;
        var avg = dailyValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return avg > 0 ? 100.0 - (calculateStandardDeviation(dailyValues) / avg) * 100 : 0.0;
    }
    
    private double calculateStandardDeviation(java.util.Collection<Double> values) {
        var avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        var variance = values.stream().mapToDouble(v -> Math.pow(v - avg, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }
    
    private List<String> calculatePeakHours(List<PaymentNotificationEntity> payments) {
        var hourlySales = payments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.createdAt.getHour(),
                        java.util.stream.Collectors.summingDouble(p -> p.amount)
                ));
        
        return hourlySales.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(java.util.Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(3)
                .map(entry -> String.format("%02d:00", entry.getKey()))
                .collect(java.util.stream.Collectors.toList());
    }
    
    private double calculateProductivityScore(double averageDailySales) {
        return Math.min(averageDailySales / 50.0, 1.0) * 100;
    }
    
    private double calculateEfficiencyRate(int confirmedCount, int totalCount) {
        return totalCount > 0 ? (double) confirmedCount / totalCount * 100 : 0.0;
    }
}

