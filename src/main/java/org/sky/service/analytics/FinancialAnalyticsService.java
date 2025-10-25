package org.sky.service.analytics;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.dto.response.stats.*;
import org.sky.dto.response.admin.ComplianceAndSecurity;
import org.sky.dto.response.admin.ComplianceStatus;
import org.sky.dto.response.admin.SecurityMetrics;
import org.sky.service.analytics.PaymentAnalyticsService.PaymentMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio especializado en análisis financiero
 * Responsabilidad única: Calcular métricas financieras y de costos
 */
@ApplicationScoped
public class FinancialAnalyticsService {
    
    private static final Logger log = Logger.getLogger(FinancialAnalyticsService.class);
    
    /**
     * Convierte Map de financialOverview a objeto FinancialOverview
     */
    public FinancialOverview convertToFinancialOverview(Map<String, Object> financialOverviewData, double totalSales) {
        if (financialOverviewData == null || financialOverviewData.isEmpty()) {
            RevenueGrowth revenueGrowth = new RevenueGrowth(0.0, 0.0, 0.0, 0.0);
            RevenueBreakdown revenueBreakdown = new RevenueBreakdown(totalSales, List.of(), revenueGrowth);
            CostAnalysis costAnalysis = new CostAnalysis(
                totalSales * 0.15, totalSales * 0.08, 5000.0, 
                totalSales * 0.77 - 5000.0, 
                totalSales > 0 ? ((totalSales * 0.77 - 5000.0) / totalSales) * 100 : 0.0
            );
            return new FinancialOverview(revenueBreakdown, costAnalysis);
        }
        
        // Extraer datos
        @SuppressWarnings("unchecked")
        Map<String, Object> revenueBreakdownData = (Map<String, Object>) financialOverviewData.get("revenueBreakdown");
        @SuppressWarnings("unchecked")
        Map<String, Object> costAnalysisData = (Map<String, Object>) financialOverviewData.get("costAnalysis");
        
        // Crear RevenueBreakdown
        List<RevenueByBranch> revenueByBranch = new ArrayList<>();
        if (revenueBreakdownData != null && revenueBreakdownData.get("revenueByBranch") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> branchData = (List<Map<String, Object>>) revenueBreakdownData.get("revenueByBranch");
            for (Map<String, Object> branch : branchData) {
                revenueByBranch.add(new RevenueByBranch(
                    getLong(branch, "branchId"),
                    getString(branch, "branchName"),
                    getDouble(branch, "revenue"),
                    getDouble(branch, "percentage")
                ));
            }
        }
        
        RevenueGrowth revenueGrowth = new RevenueGrowth(0.0, 0.0, 0.0, 0.0);
        double totalRevenue = revenueBreakdownData != null ? getDouble(revenueBreakdownData, "totalRevenue") : totalSales;
        RevenueBreakdown revenueBreakdown = new RevenueBreakdown(totalRevenue, revenueByBranch, revenueGrowth);
        
        // Crear CostAnalysis
        CostAnalysis costAnalysis = new CostAnalysis(
            costAnalysisData != null ? getDouble(costAnalysisData, "operationalCosts") : totalSales * 0.15,
            costAnalysisData != null ? getDouble(costAnalysisData, "sellerCommissions") : totalSales * 0.08,
            costAnalysisData != null ? getDouble(costAnalysisData, "systemMaintenance") : 5000.0,
            costAnalysisData != null ? getDouble(costAnalysisData, "netProfit") : totalSales * 0.77 - 5000.0,
            costAnalysisData != null ? getDouble(costAnalysisData, "profitMargin") : 0.0
        );
        
        return new FinancialOverview(revenueBreakdown, costAnalysis);
    }
    
    /**
     * Genera métricas de performance financiero
     */
    public PerformanceMetrics generatePerformanceMetrics(PaymentMetrics paymentMetrics) {
        double averageConfirmationTime = 2.3; // Tiempo promedio en minutos
        double claimRate = paymentMetrics.totalTransactions() > 0 ? 
            (double) paymentMetrics.confirmedTransactions() / paymentMetrics.totalTransactions() * 100 : 0.0;
        double rejectionRate = paymentMetrics.totalTransactions() > 0 ? 
            (double) paymentMetrics.rejectedTransactions() / paymentMetrics.totalTransactions() * 100 : 0.0;
        
        return new PerformanceMetrics(
            averageConfirmationTime,
            claimRate,
            rejectionRate,
            paymentMetrics.pendingTransactions(),
            paymentMetrics.confirmedTransactions(),
            paymentMetrics.rejectedTransactions()
        );
    }
    
    /**
     * Genera métricas del sistema de pagos
     */
    public PaymentSystemMetrics generatePaymentSystemMetrics(PaymentMetrics paymentMetrics) {
        double averageProcessingTime = 2.3; // Tiempo promedio en minutos
        double successRate = paymentMetrics.totalTransactions() > 0 ? 
            (double) paymentMetrics.confirmedTransactions() / paymentMetrics.totalTransactions() * 100 : 0.0;
        
        return new PaymentSystemMetrics(
            paymentMetrics.totalTransactions(),
            paymentMetrics.pendingTransactions(),
            paymentMetrics.confirmedTransactions(),
            paymentMetrics.rejectedTransactions(),
            averageProcessingTime,
            successRate
        );
    }
    
    /**
     * Genera métricas de salud del sistema
     */
    public OverallSystemHealth generateSystemHealth(PaymentMetrics paymentMetrics) {
        double uptime = 99.8; // Porcentaje de uptime
        double responseTime = 1.2; // Tiempo de respuesta promedio
        double errorRate = 0.0; // Tasa de errores
        
        return new OverallSystemHealth(
            paymentMetrics.totalSales(),
            paymentMetrics.totalTransactions(),
            uptime,
            responseTime,
            errorRate,
            paymentMetrics.totalTransactions()
        );
    }
    
    /**
     * Genera métricas de engagement de usuarios
     */
    public UserEngagement generateUserEngagement(PaymentMetrics paymentMetrics) {
        FeatureUsage featureUsage = new FeatureUsage(0.0, 0.0, 0.0, 0.0);
        double satisfactionScore = 4.5;
        
        return new UserEngagement(
            paymentMetrics.totalTransactions(),
            paymentMetrics.totalTransactions(),
            paymentMetrics.totalTransactions(),
            satisfactionScore,
            featureUsage
        );
    }
    
    /**
     * Genera métricas del sistema completo
     */
    public SystemMetrics generateSystemMetrics(PaymentMetrics paymentMetrics) {
        OverallSystemHealth systemHealth = generateSystemHealth(paymentMetrics);
        PaymentSystemMetrics paymentSystemMetrics = generatePaymentSystemMetrics(paymentMetrics);
        UserEngagement userEngagement = generateUserEngagement(paymentMetrics);
        
        return new SystemMetrics(systemHealth, paymentSystemMetrics, userEngagement);
    }
    
    /**
     * Genera datos de compliance y seguridad
     */
    public ComplianceAndSecurity generateComplianceAndSecurity() {
        SecurityMetrics securityMetrics = new SecurityMetrics(5L, 2L, 0L, 95.5);
        ComplianceStatus complianceStatus = new ComplianceStatus("cumple", "completo", "actualizado", "2024-01-15");
        
        return new ComplianceAndSecurity(securityMetrics, complianceStatus);
    }
    
    // ==================================================================================
    // MÉTODOS AUXILIARES
    // ==================================================================================
    
    private double getDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
    
    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : "";
    }
    
    private Long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
}