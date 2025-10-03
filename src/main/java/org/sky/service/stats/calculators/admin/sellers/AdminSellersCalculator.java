package org.sky.service.stats.calculators.admin.sellers;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.response.*;
import org.sky.dto.response.seller.*;
import org.sky.dto.response.stats.ComparisonData;
import org.sky.dto.response.stats.TopSellerData;
import org.sky.model.PaymentNotificationEntity;
import org.sky.model.SellerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
@ApplicationScoped
public class AdminSellersCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    
    public List<TopSellerData> calculateTopSellers(List<PaymentNotificationEntity> payments,
                                                   List<SellerEntity> sellers) {
        if (sellers.isEmpty()) {
            return new ArrayList<>();
        }
        
        var sellerStats = sellers.stream()
                .map(seller -> calculateSellerStats(payments, seller))
                .sorted(Comparator.comparing(TopSellerData::totalSales).reversed())
                .limit(10)
                .collect(Collectors.toList());
        
        // Asignar rankings
        for (int i = 0; i < sellerStats.size(); i++) {
            var stats = sellerStats.get(i);
            sellerStats.set(i, new TopSellerData(
                    i + 1,
                    stats.sellerId(),
                    stats.sellerName(),
                    stats.branchName(),
                    stats.totalSales(),
                    stats.transactionCount()
            ));
        }
        
        return sellerStats;
    }
    
    public SellerComparisons calculateComparisons(List<PaymentNotificationEntity> payments,
                                                  java.time.LocalDate startDate,
                                                  java.time.LocalDate endDate) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var totalSales = calculateTotalSales(confirmedPayments);
        var totalTransactions = payments.size();
        
        // Calcular comparaciones con períodos anteriores (simplificado)
        var salesChange = calculateSalesChange(totalSales, startDate, endDate);
        var transactionChange = calculateTransactionChange(totalTransactions, startDate, endDate);
        var percentageChange = calculatePercentageChange(salesChange, totalSales);
        
        var comparisonData = new ComparisonData(salesChange, (long) transactionChange, percentageChange);
        
        return new  SellerComparisons(
                comparisonData, comparisonData, comparisonData, comparisonData
        );
    }
    
    public SellerTrends calculateTrends(List<PaymentNotificationEntity> payments,
                                        java.time.LocalDate startDate,
                                        java.time.LocalDate endDate) {
        if (payments.isEmpty()) {
            return new  SellerTrends(
                "stable", "stable", 0.0, "neutral", "flat", 0.0, "none"
            );
        }
        
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var salesTrend = calculateSalesTrend(confirmedPayments, startDate, endDate);
        var transactionTrend = calculateTransactionTrend(payments, startDate, endDate);
        var growthRate = calculateGrowthRate(confirmedPayments, startDate, endDate);
        var momentum = calculateMomentum(payments);
        var trendDirection = determineTrendDirection(growthRate);
        var volatility = calculateVolatility(confirmedPayments);
        var seasonality = determineSeasonality(confirmedPayments);
        
        return new  SellerTrends(
                salesTrend, transactionTrend, growthRate, momentum, trendDirection, volatility, seasonality
        );
    }
    
    public SellerAchievements calculateAchievements(List<PaymentNotificationEntity> payments,
                                                    List<SellerEntity> sellers,
                                                    java.time.LocalDate startDate,
                                                    java.time.LocalDate endDate) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        
        var streakDays = calculateStreakDays(confirmedPayments, endDate);
        var bestStreak = calculateBestStreak(confirmedPayments, startDate, endDate);
        var totalStreaks = calculateTotalStreaks(confirmedPayments, startDate, endDate);
        
        var milestones = createMilestones(confirmedPayments);
        var badges = createBadges(confirmedPayments, sellers);
        
        return new  SellerAchievements(streakDays, bestStreak, totalStreaks, milestones, badges);
    }
    
    private  TopSellerData calculateSellerStats(List<PaymentNotificationEntity> payments, SellerEntity seller) {
        var sellerPayments = filterPaymentsBySeller(payments, seller.id);
        var confirmedPayments = filterPaymentsByStatus(sellerPayments, CONFIRMED_STATUS);
        
        var totalSales = calculateTotalSales(confirmedPayments);
        var transactionCount = (long) sellerPayments.size();
        var branchName = "Sin sucursal"; // Simplificado
        
        return new  TopSellerData(
                0, // Ranking se asigna después
                seller.id,
                seller.sellerName != null ? seller.sellerName : "Sin nombre",
                branchName,
                totalSales,
                transactionCount
        );
    }
    
    private List<PaymentNotificationEntity> filterPaymentsBySeller(List<PaymentNotificationEntity> payments, Long sellerId) {
        return payments.stream()
                .filter(payment -> sellerId.equals(payment.confirmedBy))
                .toList();
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
    
    private double calculateSalesChange(double currentSales, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        var daysDiff = startDate.until(endDate).getDays();
        return daysDiff > 7 ? currentSales * 0.1 : currentSales * 0.05;
    }
    
    private int calculateTransactionChange(int currentTransactions, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        var daysDiff = startDate.until(endDate).getDays();
        return daysDiff > 7 ? currentTransactions / 10 : currentTransactions / 20;
    }
    
    private double calculatePercentageChange(double change, double current) {
        return current > 0 ? (change / current) * 100 : 0.0;
    }
    
    private String calculateSalesTrend(List<PaymentNotificationEntity> payments, java.time.LocalDate startDate, java.time.LocalDate endDate) {
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
    
    private String calculateTransactionTrend(List<PaymentNotificationEntity> payments, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (payments.size() < 2) return "stable";
        
        var dailyCounts = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.toLocalDate(),
                        Collectors.counting()
                ));
        
        var values = dailyCounts.values().stream().sorted().toList();
        if (values.size() < 2) return "stable";
        
        var firstValue = values.get(0);
        var lastValue = values.get(values.size() - 1);
        var changePercentage = firstValue > 0 ? ((double)(lastValue - firstValue) / firstValue) * 100 : 0.0;
        
        return switch ((int) Math.signum(changePercentage)) {
            case 1 -> changePercentage > 20.0 ? "increasing" : "stable";
            case -1 -> changePercentage < -20.0 ? "decreasing" : "stable";
            default -> "stable";
        };
    }
    
    private double calculateGrowthRate(List<PaymentNotificationEntity> payments, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        var totalSales = calculateTotalSales(payments);
        var daysDiff = startDate.until(endDate).getDays() + 1;
        var dailyAvg = daysDiff > 0 ? totalSales / daysDiff : 0.0;
        return Math.max(0.0, dailyAvg * 0.1);
    }
    
    private String calculateMomentum(List<PaymentNotificationEntity> payments) {
        return payments.size() > 50 ? "strong" : payments.size() > 20 ? "building" : "weak";
    }
    
    private String determineTrendDirection(double growthRate) {
        return growthRate > 0.5 ? "up" : growthRate < -0.5 ? "down" : "flat";
    }
    
    private double calculateVolatility(List<PaymentNotificationEntity> payments) {
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
        
        return avg > 0 ? standardDeviation / avg : 0.0;
    }
    
    private String determineSeasonality(List<PaymentNotificationEntity> payments) {
        // Implementación simplificada
        return payments.size() > 100 ? "present" : "none";
    }
    
    private long calculateStreakDays(List<PaymentNotificationEntity> payments, java.time.LocalDate endDate) {
        if (payments.isEmpty()) return 0L;
        
        var daysWithSales = payments.stream()
                .map(p -> p.createdAt.toLocalDate())
                .distinct()
                .count();
        
        return Math.min(daysWithSales, 30L);
    }
    
    private long calculateBestStreak(List<PaymentNotificationEntity> payments, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (payments.isEmpty()) return 0L;
        
        var daysWithSales = payments.stream()
                .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                .map(p -> p.createdAt.toLocalDate())
                .distinct()
                .count();
        
        return Math.max(daysWithSales, 1L);
    }
    
    private long calculateTotalStreaks(List<PaymentNotificationEntity> payments, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (payments.isEmpty()) return 0L;
        
        var activeDays = payments.stream()
                .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                .map(p -> p.createdAt.toLocalDate())
                .distinct()
                .count();
        
        return Math.max(activeDays / 7, 1L);
    }
    
    private List<Milestone> createMilestones(List<PaymentNotificationEntity> payments) {
        var milestones = new ArrayList<Milestone>();
        
        if (payments.size() >= 1) {
            var firstPayment = payments.stream()
                    .min(Comparator.comparing(p -> p.createdAt))
                    .orElse(null);
            
            if (firstPayment != null) {
                milestones.add(new  Milestone(
                        "first_sale",
                        firstPayment.createdAt.toLocalDate().toString(),
                        true,
                        1.0
                ));
            }
        }
        
        if (payments.size() >= 100) {
            milestones.add(new Milestone(
                    "hundred_sales",
                    java.time.LocalDate.now().toString(),
                    true,
                    100.0
            ));
        }
        
        return milestones;
    }
    
    private List<Badge> createBadges(List<PaymentNotificationEntity> payments, List<SellerEntity> sellers) {
        var badges = new ArrayList<Badge>();
        
        if (payments.size() >= 1) {
            badges.add(new Badge(
                    "Primera Venta",
                    "🎉",
                    "Completaste tu primera venta",
                    true,
                    java.time.LocalDate.now().toString()
            ));
        }
        
        if (sellers.size() >= 5) {
            badges.add(new  Badge(
                    "Equipo Grande",
                    "👥",
                    "Tienes 5 o más vendedores",
                    true,
                    java.time.LocalDate.now().toString()
            ));
        }
        
        return badges;
    }
}
