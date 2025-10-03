package org.sky.service.stats.calculators.builder;

import org.sky.dto.response.stats.*;
import org.sky.service.stats.calculators.StatisticsCalculator.*;

import java.util.List;
import java.util.Map;

/**
 * Builder Pattern: Construye resultados de estadísticas de manera fluida
 * Clean Code: Principio de responsabilidad única - solo construye objetos de resultado
 */
public class StatsResultBuilder {
    
    private BasicStats basicStats;
    private org.sky.service.stats.calculators.StatisticsCalculator.PerformanceMetrics performanceMetrics;
    private List<DailySalesData> dailySales;
    private List<HourlySalesData> hourlySales;
    private List<WeeklySalesData> weeklySales;
    private List<MonthlySalesData> monthlySales;
        private List<TopSellerData> topSellers;
        private Map<String, Object> sellerGoals;
        private Map<String, Object> sellerPerformance;
        private Map<String, Object> systemMetrics;
        private Map<String, Object> financialOverview;
        private Map<String, Object> complianceSecurity;
    
    private StatsResultBuilder() {}
    
    public static StatsResultBuilder newBuilder() {
        return new StatsResultBuilder();
    }
    
    public StatsResultBuilder withBasicStats(BasicStats basicStats) {
        this.basicStats = basicStats;
        return this;
    }
    
    public StatsResultBuilder withPerformanceMetrics(org.sky.service.stats.calculators.StatisticsCalculator.PerformanceMetrics performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
        return this;
    }
    
    public StatsResultBuilder withDailySales(List<DailySalesData> dailySales) {
        this.dailySales = dailySales;
        return this;
    }
    
    public StatsResultBuilder withHourlySales(List<HourlySalesData> hourlySales) {
        this.hourlySales = hourlySales;
        return this;
    }
    
    public StatsResultBuilder withWeeklySales(List<WeeklySalesData> weeklySales) {
        this.weeklySales = weeklySales;
        return this;
    }
    
    public StatsResultBuilder withMonthlySales(List<MonthlySalesData> monthlySales) {
        this.monthlySales = monthlySales;
        return this;
    }
    
    public StatsResultBuilder withTopSellers(List<TopSellerData> topSellers) {
        this.topSellers = topSellers;
        return this;
    }

    public StatsResultBuilder withSellerGoals(Map<String, Object> sellerGoals) {
        this.sellerGoals = sellerGoals;
        return this;
    }

    public StatsResultBuilder withSellerPerformance(Map<String, Object> sellerPerformance) {
        this.sellerPerformance = sellerPerformance;
        return this;
    }

    public StatsResultBuilder withSystemMetrics(Map<String, Object> systemMetrics) {
        this.systemMetrics = systemMetrics;
        return this;
    }

    public StatsResultBuilder withFinancialOverview(Map<String, Object> financialOverview) {
        this.financialOverview = financialOverview;
        return this;
    }

    public StatsResultBuilder withComplianceSecurity(Map<String, Object> complianceSecurity) {
        this.complianceSecurity = complianceSecurity;
        return this;
    }
    
    public ParallelStatsResult build() {
        validateRequiredFields();
        return new ParallelStatsResult(
            basicStats,
            performanceMetrics,
            dailySales,
            hourlySales,
            weeklySales,
            monthlySales,
            topSellers,
            sellerGoals,
            sellerPerformance,
            systemMetrics,
            financialOverview,
            complianceSecurity
        );
    }
    
    private void validateRequiredFields() {
        if (basicStats == null) {
            throw new IllegalStateException("BasicStats is required");
        }
        if (performanceMetrics == null) {
            throw new IllegalStateException("PerformanceMetrics is required");
        }
        if (dailySales == null) {
            throw new IllegalStateException("DailySales is required");
        }
        if (hourlySales == null) {
            throw new IllegalStateException("HourlySales is required");
        }
        if (weeklySales == null) {
            throw new IllegalStateException("WeeklySales is required");
        }
        if (monthlySales == null) {
            throw new IllegalStateException("MonthlySales is required");
        }
        if (topSellers == null) {
            throw new IllegalStateException("TopSellers is required");
        }
    }
}
