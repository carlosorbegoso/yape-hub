package org.sky.service.stats.calculators.seller.trends;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.SellerAnalyticsRequest;
import org.sky.dto.stats.SellerTrends;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@ApplicationScoped
public class SellerTrendsCalculator {
    
    public SellerTrends calculateSellerTrends(List<PaymentNotificationEntity> sellerPayments,
                                                                     List<PaymentNotificationEntity> allPayments,
                                                                     SellerAnalyticsRequest request) {
        if (sellerPayments.isEmpty()) {
            return new SellerTrends(
                "stable", "stable", 0.0, "neutral", "flat", 0.0, "none"
            );
        }
        
        // Calcular tendencias usando period y days
        var salesTrend = calculateSalesTrend(sellerPayments, request.startDate(), request.endDate(), request.period());
        var transactionTrend = calculateTransactionTrend(sellerPayments, request.startDate(), request.endDate(), request.period());
        var growthRate = calculateGrowthRate(sellerPayments, request.days(), request.confidence());
        var momentum = calculateMomentum(sellerPayments, request.metric());
        var trendDirection = determineTrendDirection(growthRate);
        var volatility = calculateVolatility(sellerPayments, request.granularity());
        var seasonality = determineSeasonality(sellerPayments, request.period());
        
        return new SellerTrends(
                salesTrend, transactionTrend, growthRate, momentum, trendDirection, volatility, seasonality
        );
    }
    
    private String calculateSalesTrend(List<PaymentNotificationEntity> payments, LocalDate startDate, LocalDate endDate, String period) {
        if (payments.isEmpty()) {
            return "stable";
        }
        
        // Filtrar pagos confirmados en el período
        var confirmedPayments = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                .toList();
        
        if (confirmedPayments.size() < 2) {
            return "stable";
        }
        
        // Calcular ventas por período según el tipo
        var salesByPeriod = calculateSalesByPeriod(confirmedPayments, period);
        
        // Calcular tendencia basada en la variación
        return determineTrendFromData(salesByPeriod);
    }
    
    private String calculateTransactionTrend(List<PaymentNotificationEntity> payments, LocalDate startDate, LocalDate endDate, String period) {
        if (payments.isEmpty()) {
            return "stable";
        }
        
        // Filtrar pagos en el período
        var periodPayments = payments.stream()
                .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                .toList();
        
        if (periodPayments.size() < 2) {
            return "stable";
        }
        
        // Calcular transacciones por período según el tipo
        var transactionsByPeriod = calculateTransactionsByPeriod(periodPayments, period);
        
        // Calcular tendencia basada en la variación
        return determineTransactionTrendFromData(transactionsByPeriod);
    }
    
    private double calculateGrowthRate(List<PaymentNotificationEntity> payments, Integer days, Double confidence) {
        var sales = calculateTotalSales(filterPaymentsByStatus(payments, "CONFIRMED"));
        var baseDays = days != null ? days : 7;
        var dailyAvg = sales / baseDays;
        var confidenceFactor = confidence != null ? confidence : 1.0;
        return Math.max(0.0, dailyAvg * confidenceFactor * 0.1);
    }
    
    private String calculateMomentum(List<PaymentNotificationEntity> payments, String metric) {
        // Implementación real basada en el volumen de transacciones
        var transactionCount = payments.size();
        var confirmedCount = filterPaymentsByStatus(payments, "CONFIRMED").size();
        var confirmationRate = transactionCount > 0 ? (double) confirmedCount / transactionCount : 0.0;
        
        if (transactionCount > 20 && confirmationRate > 0.8) {
            return "strong";
        } else if (transactionCount > 10 && confirmationRate > 0.6) {
            return "building";
        } else {
            return "developing";
        }
    }
    
    private String determineTrendDirection(double growthRate) {
        return growthRate > 0.5 ? "up" : growthRate < -0.5 ? "down" : "flat";
    }
    
    private double calculateVolatility(List<PaymentNotificationEntity> payments, String granularity) {
        if (payments.isEmpty()) {
            return 0.0;
        }
        
        // Calcular volatilidad basada en la variación de montos
        var amounts = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .mapToDouble(p -> p.amount)
                .toArray();
        
        if (amounts.length < 2) {
            return 0.0;
        }
        
        var mean = java.util.Arrays.stream(amounts).average().orElse(0.0);
        var variance = java.util.Arrays.stream(amounts)
                .map(amount -> Math.pow(amount - mean, 2))
                .average()
                .orElse(0.0);
        
        var standardDeviation = Math.sqrt(variance);
        var coefficientOfVariation = mean > 0 ? standardDeviation / mean : 0.0;
        
        // Ajustar basado en granularity
        return granularity != null && granularity.equals("hourly") ? 
                Math.min(coefficientOfVariation * 1.5, 1.0) : 
                Math.min(coefficientOfVariation, 0.5);
    }
    
    private String determineSeasonality(List<PaymentNotificationEntity> payments, String period) {
        if (payments.isEmpty() || period == null || !period.equals("yearly")) {
            return "none";
        }
        
        // Analizar patrones estacionales basados en los datos
        var monthlyData = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.createdAt.toLocalDate().getMonth(),
                        Collectors.counting()
                ));
        
        if (monthlyData.size() < 6) {
            return "insufficient_data";
        }
        
        // Calcular coeficiente de variación mensual
        var monthlyCounts = monthlyData.values().stream().mapToLong(Long::longValue).toArray();
        var mean = java.util.Arrays.stream(monthlyCounts).average().orElse(0.0);
        var variance = java.util.Arrays.stream(monthlyCounts)
                .mapToDouble(count -> Math.pow(count - mean, 2))
                .average()
                .orElse(0.0);
        
        var coefficientOfVariation = mean > 0 ? Math.sqrt(variance) / mean : 0.0;
        
        return coefficientOfVariation > 0.3 ? "present" : "minimal";
    }
    
    // Métodos auxiliares
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
    
    private Map<String, Double> calculateSalesByPeriod(List<PaymentNotificationEntity> payments, String period) {
        return switch (period != null ? period.toLowerCase() : "daily") {
            case "weekly" -> payments.stream()
                    .collect(Collectors.groupingBy(
                            p -> getWeekKey(p.createdAt.toLocalDate()),
                            Collectors.summingDouble(p -> p.amount)
                    ));
            case "monthly" -> payments.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.createdAt.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                            Collectors.summingDouble(p -> p.amount)
                    ));
            case "yearly" -> payments.stream()
                    .collect(Collectors.groupingBy(
                            p -> String.valueOf(p.createdAt.toLocalDate().getYear()),
                            Collectors.summingDouble(p -> p.amount)
                    ));
            default -> payments.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.createdAt.toLocalDate().toString(),
                            Collectors.summingDouble(p -> p.amount)
                    ));
        };
    }
    
    private Map<String, Long> calculateTransactionsByPeriod(List<PaymentNotificationEntity> payments, String period) {
        return switch (period != null ? period.toLowerCase() : "daily") {
            case "weekly" -> payments.stream()
                    .collect(Collectors.groupingBy(
                            p -> getWeekKey(p.createdAt.toLocalDate()),
                            Collectors.counting()
                    ));
            case "monthly" -> payments.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.createdAt.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                            Collectors.counting()
                    ));
            case "yearly" -> payments.stream()
                    .collect(Collectors.groupingBy(
                            p -> String.valueOf(p.createdAt.toLocalDate().getYear()),
                            Collectors.counting()
                    ));
            default -> payments.stream()
                    .collect(Collectors.groupingBy(
                            p -> p.createdAt.toLocalDate().toString(),
                            Collectors.counting()
                    ));
        };
    }
    
    private String getWeekKey(LocalDate date) {
        var weekStart = date.with(java.time.DayOfWeek.MONDAY);
        return weekStart.format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));
    }
    
    private String determineTrendFromData(Map<String, Double> dataByPeriod) {
        if (dataByPeriod.size() < 2) {
            return "stable";
        }
        
        var values = dataByPeriod.values().stream()
                .sorted()
                .toList();
        
        if (values.isEmpty()) {
            return "stable";
        }
        
        // Calcular la tendencia basada en la diferencia entre el primer y último valor
        var firstValue = values.get(0);
        var lastValue = values.get(values.size() - 1);
        var changePercentage = firstValue > 0 ? ((lastValue - firstValue) / firstValue) * 100 : 0.0;
        
        return switch ((int) Math.signum(changePercentage)) {
            case 1 -> changePercentage > 20.0 ? "growing" : "stable";
            case -1 -> changePercentage < -20.0 ? "declining" : "stable";
            default -> "stable";
        };
    }
    
    private String determineTransactionTrendFromData(Map<String, Long> dataByPeriod) {
        if (dataByPeriod.size() < 2) {
            return "stable";
        }
        
        var values = dataByPeriod.values().stream()
                .sorted()
                .toList();
        
        if (values.isEmpty()) {
            return "stable";
        }
        
        // Calcular la tendencia basada en la diferencia entre el primer y último valor
        var firstValue = values.get(0);
        var lastValue = values.get(values.size() - 1);
        var changePercentage = firstValue > 0 ? ((double)(lastValue - firstValue) / firstValue) * 100.0 : 0.0;
        
        return switch ((int) Math.signum(changePercentage)) {
            case 1 -> changePercentage > 20.0 ? "increasing" : "stable";
            case -1 -> changePercentage < -20.0 ? "decreasing" : "stable";
            default -> "stable";
        };
    }
}
