package org.sky.service.stats.calculators.seller.goals;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.SellerAnalyticsRequest;
import org.sky.dto.stats.SellerAnalyticsResponse;
import org.sky.model.PaymentNotification;

import java.time.LocalDate;
import java.util.List;
@ApplicationScoped
public class SellerGoalsCalculator {
    
    public SellerAnalyticsResponse.SellerGoals calculateSellerGoals(List<PaymentNotification> sellerPayments, 
                                                                    List<PaymentNotification> allPayments, 
                                                                    SellerAnalyticsRequest request) {
        var totalSales = calculateTotalSales(filterPaymentsByStatus(sellerPayments, "CONFIRMED"));
        
        // Usar days para calcular objetivos dinámicos
        var baseDays = request.days() != null ? request.days() : 7;
        var dailyTarget = calculateDynamicTarget(totalSales, baseDays, request.period());
        var weeklyTarget = dailyTarget * 7;
        var monthlyTarget = dailyTarget * 30;
        var yearlyTarget = dailyTarget * 365;
        
        // Calcular progreso usando period
        var daysInPeriod = calculateDaysInPeriod(request.startDate(), request.endDate(), request.period());
        var dailyProgress = calculateProgressRate(totalSales, dailyTarget, daysInPeriod);
        var weeklyProgress = calculateProgressRate(totalSales, weeklyTarget, Math.max(1, daysInPeriod / 7));
        var monthlyProgress = calculateProgressRate(totalSales, monthlyTarget, Math.max(1, daysInPeriod / 30));
        var achievementRate = Math.min(Math.max(dailyProgress, weeklyProgress), monthlyProgress);
        
        return new SellerAnalyticsResponse.SellerGoals(
                dailyTarget, weeklyTarget, monthlyTarget, yearlyTarget,
                achievementRate, dailyProgress, weeklyProgress, monthlyProgress
        );
    }
    
    private List<PaymentNotification> filterPaymentsByStatus(List<PaymentNotification> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .toList();
    }
    
    private double calculateTotalSales(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private double calculateDynamicTarget(double totalSales, Integer days, String period) {
        var multiplier = switch (period != null ? period.toLowerCase() : "daily") {
            case "weekly" -> 7.0;
            case "monthly" -> 30.0;
            case "yearly" -> 365.0;
            default -> (double) days;
        };
        return Math.max(50.0, (totalSales / multiplier) * 1.2);
    }
    
    private int calculateDaysInPeriod(LocalDate startDate, LocalDate endDate, String period) {
        var daysDiff = startDate.until(endDate).getDays() + 1;
        return switch (period != null ? period.toLowerCase() : "daily") {
            case "weekly" -> Math.max(1, daysDiff / 7);
            case "monthly" -> Math.max(1, daysDiff / 30);
            case "yearly" -> Math.max(1, daysDiff / 365);
            default -> daysDiff;
        };
    }
    
    private double calculateProgressRate(double actual, double target, int periodDays) {
        var expected = target * periodDays;
        return expected > 0 ? Math.min(actual / expected, 2.0) : 0.0;
    }
}

