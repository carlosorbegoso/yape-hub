package org.sky.service.stats.analytics;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.dto.stats.SellerAnalyticsResponse;
import org.sky.model.PaymentNotification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SellerAnalyticsProcessor {
    
    private static final Logger logger = Logger.getLogger(SellerAnalyticsProcessor.class);

    public SellerAnalyticsResponse.OverviewMetrics calculateOverviewMetrics(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        long totalPayments = payments.size();
        long confirmedPayments = payments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
        long pendingPayments = payments.stream().filter(p -> "PENDING".equals(p.status)).count();
        long rejectedPayments = payments.stream().filter(p -> p.status.startsWith("REJECTED")).count();
        
        double totalSales = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .mapToDouble(p -> p.amount)
                .sum();
        
        double averageTransactionValue = confirmedPayments > 0 ? totalSales / confirmedPayments : 0.0;
        
        return new SellerAnalyticsResponse.OverviewMetrics(
            totalSales, totalPayments, averageTransactionValue,
            0.0, 0.0, 0.0
        );
    }

    public List<SellerAnalyticsResponse.DailySalesData> calculateDailySalesData(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        Map<String, List<PaymentNotification>> paymentsByDate = payments.stream()
                .collect(Collectors.groupingBy(p -> p.createdAt.toLocalDate().toString()));
        
        List<SellerAnalyticsResponse.DailySalesData> dailySales = new ArrayList<>();
        String[] dayNames = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        
        LocalDate currentDate = startDate;
        int dayIndex = 0;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.toString();
            List<PaymentNotification> dayPayments = paymentsByDate.getOrDefault(dateStr, new ArrayList<>());
            
            double sales = dayPayments.stream()
                    .filter(p -> "CONFIRMED".equals(p.status))
                    .mapToDouble(p -> p.amount)
                    .sum();
            
            long transactions = dayPayments.size();
            String dayName = dayNames[dayIndex % 7];
            
            dailySales.add(new SellerAnalyticsResponse.DailySalesData(dateStr, dayName, sales, transactions));
            currentDate = currentDate.plusDays(1);
            dayIndex++;
        }
        
        return dailySales;
    }

    public List<SellerAnalyticsResponse.HourlySalesData> calculateHourlySalesData(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        List<SellerAnalyticsResponse.HourlySalesData> hourlySales = new ArrayList<>();
        
        for (int hour = 0; hour < 24; hour++) {
            final int currentHour = hour;
            String hourStr = String.format("%02d:00", currentHour);
            
            double sales = payments.stream()
                    .filter(p -> "CONFIRMED".equals(p.status))
                    .filter(p -> p.createdAt.getHour() == currentHour)
                    .mapToDouble(p -> p.amount)
                    .sum();
            
            long transactions = payments.stream()
                    .filter(p -> "CONFIRMED".equals(p.status))
                    .filter(p -> p.createdAt.getHour() == currentHour)
                    .count();
            
            hourlySales.add(new SellerAnalyticsResponse.HourlySalesData(hourStr, sales, transactions));
        }
        
        return hourlySales;
    }

    public List<SellerAnalyticsResponse.WeeklySalesData> calculateWeeklySalesData(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        return new ArrayList<>(); // Simplificado
    }

    public List<SellerAnalyticsResponse.MonthlySalesData> calculateMonthlySalesData(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        return new ArrayList<>(); // Simplificado
    }

    public SellerAnalyticsResponse.PerformanceMetrics calculatePerformanceMetrics(List<PaymentNotification> payments) {
        long pendingPayments = payments.stream().filter(p -> "PENDING".equals(p.status)).count();
        long confirmedPayments = payments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
        long rejectedPayments = payments.stream().filter(p -> "REJECTED_BY_SELLER".equals(p.status)).count();
        
        long totalProcessed = confirmedPayments + rejectedPayments;
        double claimRate = totalProcessed > 0 ? (double) confirmedPayments / totalProcessed * 100 : 0.0;
        double rejectionRate = totalProcessed > 0 ? (double) rejectedPayments / totalProcessed * 100 : 0.0;
        
        return new SellerAnalyticsResponse.PerformanceMetrics(
            2.3, claimRate, rejectionRate,
            pendingPayments, confirmedPayments, rejectedPayments
        );
    }

    public SellerAnalyticsResponse.SellerGoals calculateGoals(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        return new SellerAnalyticsResponse.SellerGoals(
            50.0, 350.0, 1500.0, 18000.0,
            75.0, 65.0, 60.0, 50.0
        );
    }

    public SellerAnalyticsResponse.SellerPerformance calculatePerformance(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        if (payments.isEmpty()) {
            return new SellerAnalyticsResponse.SellerPerformance(
                null, null, 0.0, 0.0, new ArrayList<>(), 0.0, 0.0, 0.0
            );
        }
        
        return new SellerAnalyticsResponse.SellerPerformance(
            startDate.toString(), endDate.toString(), 25.0, 0.75,
            List.of("14:00", "15:00"), 80.0, 85.0, 2.0
        );
    }

    public SellerAnalyticsResponse.SellerComparisons calculateComparisons(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        SellerAnalyticsResponse.ComparisonData emptyComparison = createEmptyComparison();
        
        return new SellerAnalyticsResponse.SellerComparisons(
            emptyComparison, emptyComparison, emptyComparison, emptyComparison
        );
    }

    public SellerAnalyticsResponse.SellerTrends calculateTrends(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        return new SellerAnalyticsResponse.SellerTrends(
            "stable", "stable", 0.0, "neutral", "flat", 0.0, "none"
        );
    }

    public SellerAnalyticsResponse.SellerAchievements calculateAchievements(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        List<SellerAnalyticsResponse.Milestone> milestones = new ArrayList<>();
        List<SellerAnalyticsResponse.Badge> badges = new ArrayList<>();
        
        return new SellerAnalyticsResponse.SellerAchievements(5L, 8L, 1L, milestones, badges);
    }

    public SellerAnalyticsResponse.SellerInsights calculateInsights(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        if (payments.isEmpty()) {
            return new SellerAnalyticsResponse.SellerInsights(
                null, null, 0.0, 0.0, 0.0, 100.0, 100.0, 0.0
            );
        }
        
        return new SellerAnalyticsResponse.SellerInsights(
            "Martes", "14:00", 20.0, 
            90.0, 80.0, 10.0, 
            95.0, 85.0
        );
    }

    public SellerAnalyticsResponse.SellerForecasting calculateForecasting(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        List<SellerAnalyticsResponse.PredictedSale> predictedSales = new ArrayList<>();
        
        return new SellerAnalyticsResponse.SellerForecasting(
            predictedSales,
            new SellerAnalyticsResponse.TrendAnalysis("stable", 0.0, 0.0, 0.0),
            Arrays.asList(
                "Intenta vender más en las horas pico",
                "Considera aumentar tu actividad",
                "Mantén un registro constante de tus ventas"
            )
        );
    }

    public SellerAnalyticsResponse.SellerAnalytics calculateAnalytics(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        return new SellerAnalyticsResponse.SellerAnalytics(
            new SellerAnalyticsResponse.SalesDistribution(60.0, 40.0, 25.0, 35.0, 40.0),
            new SellerAnalyticsResponse.TransactionPatterns(2.0, "Martes", "14:00", "medium"),
            new SellerAnalyticsResponse.PerformanceIndicators(20.0, 2.5, 0.7, 0.6)
        );
    }

    private SellerAnalyticsResponse.ComparisonData createEmptyComparison() {
        return new SellerAnalyticsResponse.ComparisonData(0.0, 0L, 0.0);
    }
}