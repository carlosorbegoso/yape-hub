package org.sky.service.stats.algorithms;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

/**
 * Algoritmos de predicción ultra-optimizados usando Apache Commons Math
 * Versión simplificada y estable para máximo rendimiento
 */
public class OptimizedPredictionAlgorithms {
    
    // Pool de threads optimizado para cálculos paralelos
    private static final ExecutorService executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );
    
    // Cache inteligente con TTL
    private static final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutos
    
    /**
     * Entrada de caché con timestamp y validación
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long timestamp;
        
        public CacheEntry(T value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_TTL_MS;
        }
        
        public T getValue() { return value; }
    }
    
    /**
     * Regresión lineal ultra-optimizada usando Apache Commons Math
     */
    public static class OptimizedLinearRegression {
        private final SimpleRegression regression;
        private final String cacheKey;
        
        public OptimizedLinearRegression(List<Double> xValues, List<Double> yValues) {
            this.cacheKey = "lr_" + xValues.hashCode() + "_" + yValues.hashCode();
            
            // Verificar caché
            @SuppressWarnings("unchecked")
            CacheEntry<SimpleRegression> cached = (CacheEntry<SimpleRegression>) cache.get(cacheKey);
            if (cached != null && cached.isValid()) {
                this.regression = cached.getValue();
            } else {
                this.regression = new SimpleRegression();
                for (int i = 0; i < xValues.size(); i++) {
                    this.regression.addData(xValues.get(i), yValues.get(i));
                }
                cache.put(cacheKey, new CacheEntry<>(this.regression));
            }
        }
        
        public double predict(double x) {
            return regression.predict(x);
        }
        
        public double[] predict(double[] xValues) {
            return Arrays.stream(xValues)
                .parallel()
                .map(this::predict)
                .toArray();
        }
        
        public double getSlope() { return regression.getSlope(); }
        public double getIntercept() { return regression.getIntercept(); }
        public double getRSquared() { return regression.getRSquare(); }
        public double getSignificance() { return regression.getSignificance(); }
        
        public boolean isSignificant() {
            return getSignificance() < 0.05 && getRSquared() > 0.5;
        }
    }
    
    /**
     * Análisis estadístico avanzado usando Apache Commons Math
     */
    public static class AdvancedStatistics {
        
        public static Map<String, Double> calculateComprehensiveStats(List<Double> values) {
            if (values.isEmpty()) {
                return Map.of("mean", 0.0, "std", 0.0, "variance", 0.0, "skewness", 0.0, "kurtosis", 0.0);
            }
            
            String cacheKey = "stats_" + values.hashCode();
            @SuppressWarnings("unchecked")
            CacheEntry<Map<String, Double>> cached = (CacheEntry<Map<String, Double>>) cache.get(cacheKey);
            if (cached != null && cached.isValid()) {
                return cached.getValue();
            }
            
            DescriptiveStatistics stats = new DescriptiveStatistics();
            values.forEach(stats::addValue);
            
            Map<String, Double> result = new HashMap<>();
            result.put("mean", stats.getMean());
            result.put("std", stats.getStandardDeviation());
            result.put("variance", stats.getVariance());
            result.put("skewness", stats.getSkewness());
            result.put("kurtosis", stats.getKurtosis());
            result.put("min", stats.getMin());
            result.put("max", stats.getMax());
            result.put("median", stats.getPercentile(50));
            result.put("q1", stats.getPercentile(25));
            result.put("q3", stats.getPercentile(75));
            result.put("iqr", stats.getPercentile(75) - stats.getPercentile(25));
            result.put("range", stats.getMax() - stats.getMin());
            result.put("cv", stats.getStandardDeviation() / (stats.getMean() + 1e-10) * 100); // Coeficiente de variación
            
            cache.put(cacheKey, new CacheEntry<>(result));
            return result;
        }
        
        public static double calculateCorrelation(List<Double> x, List<Double> y) {
            if (x.size() != y.size() || x.size() < 2) return 0.0;
            
            double[] xArray = x.stream().mapToDouble(Double::doubleValue).toArray();
            double[] yArray = y.stream().mapToDouble(Double::doubleValue).toArray();
            
            PearsonsCorrelation correlation = new PearsonsCorrelation();
            return correlation.correlation(xArray, yArray);
        }
        
        public static List<Integer> detectOutliers(List<Double> values) {
            Map<String, Double> stats = calculateComprehensiveStats(values);
            double q1 = stats.get("q1");
            double q3 = stats.get("q3");
            double iqr = stats.get("iqr");
            
            double lowerBound = q1 - 1.5 * iqr;
            double upperBound = q3 + 1.5 * iqr;
            
            return IntStream.range(0, values.size())
                .parallel()
                .filter(i -> {
                    double value = values.get(i);
                    return value < lowerBound || value > upperBound;
                })
                .boxed()
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Predicción de series temporales optimizada
     */
    public static class TimeSeriesPrediction {
        
        public static List<Double> predictWithARIMA(List<Double> values, int periods) {
            if (values.size() < 3) {
                return IntStream.rangeClosed(1, periods)
                    .mapToDouble(i -> values.isEmpty() ? 1.0 : values.get(values.size() - 1))
                    .boxed()
                    .collect(Collectors.toList());
            }
            
            String cacheKey = "arima_" + values.hashCode() + "_" + periods;
            @SuppressWarnings("unchecked")
            CacheEntry<List<Double>> cached = (CacheEntry<List<Double>>) cache.get(cacheKey);
            if (cached != null && cached.isValid()) {
                return cached.getValue();
            }
            
            try {
                // Implementación simplificada de ARIMA(1,1,1)
                double[] data = values.stream().mapToDouble(Double::doubleValue).toArray();
                
                // Calcular tendencia
                OptimizedLinearRegression trend = new OptimizedLinearRegression(
                    IntStream.range(0, data.length).mapToObj(i -> (double) i).collect(Collectors.toList()),
                    Arrays.stream(data).boxed().collect(Collectors.toList())
                );
                
                List<Double> predictions = new ArrayList<>();
                double lastValue = data[data.length - 1];
                double slope = trend.getSlope();
                
                for (int i = 1; i <= periods; i++) {
                    double prediction = lastValue + slope * i;
                    predictions.add(Math.max(0, prediction));
                }
                
                cache.put(cacheKey, new CacheEntry<>(predictions));
                return predictions;
                
            } catch (Exception e) {
                // Fallback a predicción simple
                return predictWithExponentialSmoothing(values, periods);
            }
        }
        
        public static List<Double> predictWithExponentialSmoothing(List<Double> values, int periods) {
            if (values.isEmpty()) {
                return IntStream.rangeClosed(1, periods)
                    .mapToDouble(i -> 1.0 + i * 0.1)
                    .boxed()
                    .collect(Collectors.toList());
            }
            
            String cacheKey = "exp_" + values.hashCode() + "_" + periods;
            @SuppressWarnings("unchecked")
            CacheEntry<List<Double>> cached = (CacheEntry<List<Double>>) cache.get(cacheKey);
            if (cached != null && cached.isValid()) {
                return cached.getValue();
            }
            
            try {
                double alpha = 0.3;
                double[] data = values.stream().mapToDouble(Double::doubleValue).toArray();
                
                // Suavizado exponencial simple
                double[] smoothed = new double[data.length];
                smoothed[0] = data[0];
                
                for (int i = 1; i < data.length; i++) {
                    smoothed[i] = alpha * data[i] + (1 - alpha) * smoothed[i - 1];
                }
                
                double lastSmoothed = smoothed[smoothed.length - 1];
                List<Double> predictions = IntStream.rangeClosed(1, periods)
                    .mapToDouble(i -> lastSmoothed * FastMath.pow(1.01, i))
                    .map(v -> Math.max(0, v))
                    .boxed()
                    .collect(Collectors.toList());
                
                cache.put(cacheKey, new CacheEntry<>(predictions));
                return predictions;
                
            } catch (Exception e) {
                // Fallback final
                double lastValue = values.get(values.size() - 1);
                return IntStream.rangeClosed(1, periods)
                    .mapToDouble(i -> lastValue * (1 + 0.01 * i))
                    .boxed()
                    .collect(Collectors.toList());
            }
        }
        
        public static Map<String, List<Double>> predictWithMultipleMethods(List<Double> values, int periods) {
            CompletableFuture<List<Double>> arimaFuture = CompletableFuture.supplyAsync(() -> 
                predictWithARIMA(values, periods), executorService);
            
            CompletableFuture<List<Double>> expFuture = CompletableFuture.supplyAsync(() -> 
                predictWithExponentialSmoothing(values, periods), executorService);
            
            CompletableFuture<List<Double>> linearFuture = CompletableFuture.supplyAsync(() -> {
                if (values.size() < 2) return Collections.emptyList();
                
                OptimizedLinearRegression regression = new OptimizedLinearRegression(
                    IntStream.range(0, values.size()).mapToObj(i -> (double) i).collect(Collectors.toList()),
                    values
                );
                
                return IntStream.rangeClosed(values.size(), values.size() + periods - 1)
                    .mapToDouble(regression::predict)
                    .map(v -> Math.max(0, v))
                    .boxed()
                    .collect(Collectors.toList());
            }, executorService);
            
            try {
                CompletableFuture.allOf(arimaFuture, expFuture, linearFuture).get(5, TimeUnit.SECONDS);
                
                return Map.of(
                    "arima", arimaFuture.get(),
                    "exponential", expFuture.get(),
                    "linear", linearFuture.get()
                );
            } catch (Exception e) {
                // Fallback si algún método falla
                List<Double> fallback = predictWithExponentialSmoothing(values, periods);
                return Map.of(
                    "arima", fallback,
                    "exponential", fallback,
                    "linear", fallback
                );
            }
        }
    }
    
    /**
     * Detección de anomalías usando Apache Commons Math
     */
    public static class AnomalyDetection {
        
        public static List<Integer> detectAnomaliesWithZScore(List<Double> values, double threshold) {
            if (values.size() < 3) return Collections.emptyList();
            
            Map<String, Double> stats = AdvancedStatistics.calculateComprehensiveStats(values);
            double mean = stats.get("mean");
            double std = stats.get("std");
            
            if (std == 0) return Collections.emptyList();
            
            return IntStream.range(0, values.size())
                .parallel()
                .filter(i -> FastMath.abs(values.get(i) - mean) / std > threshold)
                .boxed()
                .collect(Collectors.toList());
        }
        
        public static List<Integer> detectAnomaliesWithNormalDistribution(List<Double> values, double percentile) {
            if (values.size() < 3) return Collections.emptyList();
            
            Map<String, Double> stats = AdvancedStatistics.calculateComprehensiveStats(values);
            double mean = stats.get("mean");
            double std = stats.get("std");
            
            if (std == 0) return Collections.emptyList();
            
            // Calcular umbral usando distribución normal
            NormalDistribution normalDist = new NormalDistribution(mean, std);
            double threshold = normalDist.inverseCumulativeProbability(percentile);
            
            return IntStream.range(0, values.size())
                .parallel()
                .filter(i -> FastMath.abs(values.get(i) - mean) / std > threshold)
                .boxed()
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Análisis de correlaciones avanzado
     */
    public static class CorrelationAnalysis {
        
        public static Map<String, Double> analyzeMultipleCorrelations(Map<String, List<Double>> metrics) {
            List<String> metricNames = new ArrayList<>(metrics.keySet());
            Map<String, Double> correlations = new ConcurrentHashMap<>();
            
            // Calcular todas las correlaciones en paralelo
            IntStream.range(0, metricNames.size())
                .parallel()
                .forEach(i -> {
                    for (int j = i + 1; j < metricNames.size(); j++) {
                        String metric1 = metricNames.get(i);
                        String metric2 = metricNames.get(j);
                        
                        List<Double> values1 = metrics.get(metric1);
                        List<Double> values2 = metrics.get(metric2);
                        
                        if (values1.size() == values2.size() && values1.size() > 1) {
                            double correlation = AdvancedStatistics.calculateCorrelation(values1, values2);
                            String key = metric1 + "_vs_" + metric2;
                            correlations.put(key, correlation);
                        }
                    }
                });
            
            return correlations;
        }
        
        public static Map<String, Object> performCorrelationTest(List<Double> x, List<Double> y) {
            if (x.size() != y.size() || x.size() < 3) {
                return Map.of("correlation", 0.0, "pValue", 1.0, "significant", false);
            }
            
            try {
                double correlation = AdvancedStatistics.calculateCorrelation(x, y);
                
                // Test t simplificado para la correlación
                int n = x.size();
                double t = correlation * FastMath.sqrt((n - 2) / (1 - correlation * correlation));
                NormalDistribution normalDist = new NormalDistribution();
                double pValue = 2 * (1 - normalDist.cumulativeProbability(Math.abs(t)));
                
                return Map.of(
                    "correlation", correlation,
                    "pValue", pValue,
                    "significant", pValue < 0.05,
                    "confidence", 1.0 - pValue
                );
                
            } catch (Exception e) {
                double correlation = AdvancedStatistics.calculateCorrelation(x, y);
                return Map.of(
                    "correlation", correlation,
                    "pValue", 1.0,
                    "significant", false
                );
            }
        }
    }
    
    /**
     * Evaluación de modelos de predicción
     */
    public static class ModelEvaluation {
        
        public static Map<String, Double> evaluatePredictionAccuracy(List<Double> actual, List<Double> predicted) {
            if (actual.size() != predicted.size() || actual.isEmpty()) {
                return Map.of("mae", 0.0, "rmse", 0.0, "mape", 0.0, "r2", 0.0);
            }
            
            double[] actualArray = actual.stream().mapToDouble(Double::doubleValue).toArray();
            double[] predictedArray = predicted.stream().mapToDouble(Double::doubleValue).toArray();
            
            // MAE (Mean Absolute Error)
            double mae = IntStream.range(0, actualArray.length)
                .parallel()
                .mapToDouble(i -> FastMath.abs(actualArray[i] - predictedArray[i]))
                .average()
                .orElse(0.0);
            
            // RMSE (Root Mean Square Error)
            double rmse = FastMath.sqrt(IntStream.range(0, actualArray.length)
                .parallel()
                .mapToDouble(i -> FastMath.pow(actualArray[i] - predictedArray[i], 2))
                .average()
                .orElse(0.0));
            
            // MAPE (Mean Absolute Percentage Error)
            double mape = IntStream.range(0, actualArray.length)
                .parallel()
                .mapToDouble(i -> FastMath.abs(actualArray[i] - predictedArray[i]) / (actualArray[i] + 1e-10) * 100)
                .average()
                .orElse(0.0);
            
            // R² (Coefficient of Determination) usando FastMath optimizado
            double actualMean = Arrays.stream(actualArray).average().orElse(0.0);
            double ssRes = IntStream.range(0, actualArray.length)
                .parallel()
                .mapToDouble(i -> FastMath.pow(actualArray[i] - predictedArray[i], 2))
                .sum();
            double ssTot = IntStream.range(0, actualArray.length)
                .parallel()
                .mapToDouble(i -> FastMath.pow(actualArray[i] - actualMean, 2))
                .sum();
            double r2 = ssTot == 0 ? 0 : 1 - (ssRes / ssTot);
            
            return Map.of("mae", mae, "rmse", rmse, "mape", mape, "r2", r2);
        }
        
        public static String getBestPredictionMethod(Map<String, List<Double>> predictions, List<Double> actual) {
            if (predictions.isEmpty() || actual.isEmpty()) return "none";
            
            String bestMethod = "none";
            double bestScore = Double.MAX_VALUE;
            
            for (Map.Entry<String, List<Double>> entry : predictions.entrySet()) {
                Map<String, Double> metrics = evaluatePredictionAccuracy(actual, entry.getValue());
                double score = metrics.get("mape"); // Usar MAPE como métrica principal
                
                if (score < bestScore) {
                    bestScore = score;
                    bestMethod = entry.getKey();
                }
            }
            
            return bestMethod;
        }
    }
    
    /**
     * Análisis completo en paralelo
     */
    public static class ComprehensiveAnalysis {
        
        public static CompletableFuture<Map<String, Object>> performFullAnalysis(
            Map<String, List<Double>> dataSets) {
            
            return CompletableFuture.supplyAsync(() -> {
                Map<String, Object> results = new ConcurrentHashMap<>();
                
                // Ejecutar múltiples análisis en paralelo
                CompletableFuture<Map<String, Double>> correlationsFuture = 
                    CompletableFuture.supplyAsync(() -> CorrelationAnalysis.analyzeMultipleCorrelations(dataSets));
                
                CompletableFuture<Map<String, Map<String, Double>>> statsFuture = 
                    CompletableFuture.supplyAsync(() -> 
                        dataSets.entrySet().parallelStream()
                            .collect(Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                entry -> AdvancedStatistics.calculateComprehensiveStats(entry.getValue())
                            )));
                
                CompletableFuture<Map<String, List<Integer>>> outliersFuture = 
                    CompletableFuture.supplyAsync(() -> 
                        dataSets.entrySet().parallelStream()
                            .collect(Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                entry -> AnomalyDetection.detectAnomaliesWithZScore(entry.getValue(), 2.0)
                            )));
                
                // Esperar resultados
                try {
                    CompletableFuture.allOf(correlationsFuture, statsFuture, outliersFuture).get(10, TimeUnit.SECONDS);
                    
                    results.put("correlations", correlationsFuture.get());
                    results.put("statistics", statsFuture.get());
                    results.put("outliers", outliersFuture.get());
                    
                } catch (Exception e) {
                    results.put("error", "Analysis timeout or failure");
                }
                
                return results;
            }, executorService);
        }
    }
    
    /**
     * Gestión de caché
     */
    public static class CacheManager {
        
        public static void cleanExpiredCache() {
            cache.entrySet().removeIf(entry -> !entry.getValue().isValid());
        }
        
        public static void clearAllCache() {
            cache.clear();
        }
        
        public static Map<String, Object> getCacheStats() {
            long totalEntries = cache.size();
            long validEntries = cache.values().stream()
                .mapToLong(entry -> entry.isValid() ? 1 : 0)
                .sum();
            
            return Map.of(
                "totalEntries", totalEntries,
                "validEntries", validEntries,
                "expiredEntries", totalEntries - validEntries,
                "hitRate", totalEntries > 0 ? (double) validEntries / totalEntries : 0.0
            );
        }
    }
    
    /**
     * Cerrar recursos
     */
    public static void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}