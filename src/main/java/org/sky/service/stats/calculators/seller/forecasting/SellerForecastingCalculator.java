package org.sky.service.stats.calculators.seller.forecasting;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@ApplicationScoped
public class SellerForecastingCalculator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public SellerForecasting calculateSellerForecasting(List<PaymentNotification> sellerPayments, 
                                                                               List<PaymentNotification> allPayments, 
                                                                               SellerAnalyticsRequest request) {
        if (sellerPayments.isEmpty()) {
            var emptyTrend = new TrendAnalysis("stable", 0.0, 0.0, 0.0);
            return new SellerForecasting(List.of(), emptyTrend, List.of());
        }
        
        // Calcular pronósticos usando period y days
        createPredictedSales(sellerPayments, request.startDate(), request.endDate(), request.period(), request.days());
        var trendAnalysis = createTrendAnalysis(sellerPayments, request.period(), request.confidence());
        createRecommendations(sellerPayments, request.include(), request.metric());
        
        return new SellerForecasting(
                List.of(), 
                trendAnalysis, 
                List.of()
        );
    }
    
    private List<Object> createPredictedSales(List<PaymentNotification> payments, 
                                             LocalDate startDate, LocalDate endDate, 
                                             String period, Integer days) {
        var predictions = new ArrayList<Object>();
        
        // Calcular datos históricos para el pronóstico
        var historicalData = calculateHistoricalData(payments, startDate, endDate, period);
        var trend = calculateTrendFromHistoricalData(historicalData);
        var seasonality = calculateSeasonalityFactor(historicalData, period);
        
        // Generar pronósticos para los próximos días
        var forecastDays = days != null ? days : 7;
        var currentDate = endDate.plusDays(1);
        
        for (int i = 0; i < forecastDays; i++) {
            var forecastDate = currentDate.plusDays(i);
            var baseValue = calculateBaseForecastValue(historicalData, period);
            var trendAdjustment = trend * (i + 1);
            var seasonalityAdjustment = seasonality * getSeasonalityMultiplier(forecastDate, period);
            
            var predictedValue = Math.max(0.0, baseValue + trendAdjustment + seasonalityAdjustment);
            var confidence = calculateForecastConfidence(historicalData.size(), trend, seasonality);
            
            // Crear objeto de predicción simple
            var prediction = Map.of(
                "date", forecastDate.format(DATE_FORMATTER),
                "predictedValue", predictedValue,
                "confidence", confidence
            );
            predictions.add(prediction);
        }
        
        return predictions;
    }
    
    private TrendAnalysis createTrendAnalysis(List<PaymentNotification> payments, 
                                                                     String period, Double confidence) {
        var historicalData = calculateHistoricalData(payments, 
                payments.stream().map(p -> p.createdAt.toLocalDate()).min(LocalDate::compareTo).orElse(LocalDate.now().minusDays(30)),
                payments.stream().map(p -> p.createdAt.toLocalDate()).max(LocalDate::compareTo).orElse(LocalDate.now()),
                period);
        
        var trend = calculateTrendFromHistoricalData(historicalData);
        var volatility = calculateVolatilityFromHistoricalData(historicalData);
        var momentum = calculateMomentumFromHistoricalData(historicalData);
        var confidenceFactor = confidence != null ? confidence : 1.0;
        
        var adjustedTrend = trend * confidenceFactor;
        var adjustedVolatility = volatility * confidenceFactor;
        var adjustedMomentum = momentum * confidenceFactor;
        
        return new TrendAnalysis(
                determineTrendDirection(adjustedTrend),
                adjustedTrend,
                adjustedVolatility,
                adjustedMomentum
        );
    }
    
    private List<Object> createRecommendations(List<PaymentNotification> payments, 
                                              String include, String metric) {
        var recommendations = new ArrayList<Object>();
        
        // Analizar patrones de rendimiento
        var performanceAnalysis = analyzePerformancePatterns(payments, metric);
        
        // Generar recomendaciones basadas en el análisis
        if (performanceAnalysis.get("lowVolume")) {
            recommendations.add(Map.of(
                "type", "increase_activity",
                "title", "Aumentar actividad de ventas",
                "description", "Considera ampliar horarios de atención o mejorar promociones",
                "priority", 0.8
            ));
        }
        
        if (performanceAnalysis.get("highRejectionRate")) {
            recommendations.add(Map.of(
                "type", "improve_quality",
                "title", "Mejorar calidad del servicio",
                "description", "Revisa procesos de confirmación y atención al cliente",
                "priority", 0.9
            ));
        }
        
        if (performanceAnalysis.get("inconsistentPerformance")) {
            recommendations.add(Map.of(
                "type", "stabilize_operations",
                "title", "Estabilizar operaciones",
                "description", "Implementa rutinas consistentes y monitorea métricas clave",
                "priority", 0.7
            ));
        }
        
        // Agregar recomendaciones basadas en include
        if (include != null && include.contains("growth")) {
            recommendations.add(Map.of(
                "type", "growth_strategy",
                "title", "Estrategia de crecimiento",
                "description", "Desarrolla estrategias para expandir base de clientes",
                "priority", 0.6
            ));
        }
        
        return recommendations;
    }
    
    // Métodos auxiliares para pronósticos
    private Map<String, Double> calculateHistoricalData(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate, String period) {
        return switch (period != null ? period.toLowerCase() : "daily") {
            case "weekly" -> payments.stream()
                    .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                    .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                    .collect(Collectors.groupingBy(
                            p -> getWeekKey(p.createdAt.toLocalDate()),
                            Collectors.summingDouble(p -> p.amount)
                    ));
            case "monthly" -> payments.stream()
                    .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                    .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                    .collect(Collectors.groupingBy(
                            p -> p.createdAt.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                            Collectors.summingDouble(p -> p.amount)
                    ));
            default -> payments.stream()
                    .filter(p -> !p.createdAt.toLocalDate().isBefore(startDate))
                    .filter(p -> !p.createdAt.toLocalDate().isAfter(endDate))
                    .collect(Collectors.groupingBy(
                            p -> p.createdAt.toLocalDate().toString(),
                            Collectors.summingDouble(p -> p.amount)
                    ));
        };
    }
    
    private double calculateTrendFromHistoricalData(Map<String, Double> historicalData) {
        if (historicalData.size() < 2) {
            return 0.0;
        }
        
        var values = historicalData.values().stream()
                .sorted()
                .toList();
        
        var firstValue = values.get(0);
        var lastValue = values.get(values.size() - 1);
        
        return firstValue > 0 ? (lastValue - firstValue) / firstValue : 0.0;
    }
    
    private double calculateSeasonalityFactor(Map<String, Double> historicalData, String period) {
        if (historicalData.size() < 4) {
            return 0.0;
        }
        
        var values = historicalData.values().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        
        var mean = java.util.Arrays.stream(values).average().orElse(0.0);
        var variance = java.util.Arrays.stream(values)
                .map(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance) / mean;
    }
    
    private double calculateBaseForecastValue(Map<String, Double> historicalData, String period) {
        return historicalData.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    private double getSeasonalityMultiplier(LocalDate date, String period) {
        return switch (period != null ? period.toLowerCase() : "daily") {
            case "weekly" -> date.getDayOfWeek().getValue() / 7.0;
            case "monthly" -> date.getDayOfMonth() / 30.0;
            default -> 1.0;
        };
    }
    
    private double calculateForecastConfidence(int dataPoints, double trend, double seasonality) {
        var baseConfidence = Math.min(0.9, dataPoints / 30.0);
        var trendConfidence = Math.abs(trend) < 0.5 ? 0.1 : 0.0;
        var seasonalityConfidence = seasonality < 0.3 ? 0.1 : 0.0;
        
        return Math.max(0.1, baseConfidence - trendConfidence - seasonalityConfidence);
    }
    
    private double calculateVolatilityFromHistoricalData(Map<String, Double> historicalData) {
        if (historicalData.size() < 2) {
            return 0.0;
        }
        
        var values = historicalData.values().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        
        var mean = java.util.Arrays.stream(values).average().orElse(0.0);
        var variance = java.util.Arrays.stream(values)
                .map(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    private double calculateMomentumFromHistoricalData(Map<String, Double> historicalData) {
        if (historicalData.size() < 3) {
            return 0.0;
        }
        
        var values = historicalData.values().stream()
                .sorted()
                .toList();
        
        var recent = values.get(values.size() - 1);
        var previous = values.get(values.size() - 2);
        var older = values.get(values.size() - 3);
        
        var recentChange = previous > 0 ? (recent - previous) / previous : 0.0;
        var previousChange = older > 0 ? (previous - older) / older : 0.0;
        
        return recentChange - previousChange;
    }
    
    private String determineTrendDirection(double trend) {
        return trend > 0.1 ? "upward" : trend < -0.1 ? "downward" : "stable";
    }
    
    private Map<String, Boolean> analyzePerformancePatterns(List<PaymentNotification> payments, String metric) {
        var totalPayments = payments.size();
        var confirmedPayments = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .count();
        var rejectedPayments = payments.stream()
                .filter(p -> "REJECTED_BY_SELLER".equals(p.status))
                .count();
        
        var confirmationRate = totalPayments > 0 ? (double) confirmedPayments / totalPayments : 0.0;
        var rejectionRate = totalPayments > 0 ? (double) rejectedPayments / totalPayments : 0.0;
        
        return Map.of(
                "lowVolume", totalPayments < 10,
                "highRejectionRate", rejectionRate > 0.2,
                "inconsistentPerformance", confirmationRate < 0.7
        );
    }
    
    private String getWeekKey(LocalDate date) {
        var weekStart = date.with(java.time.DayOfWeek.MONDAY);
        return weekStart.format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));
    }
}
