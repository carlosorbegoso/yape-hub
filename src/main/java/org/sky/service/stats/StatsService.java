package org.sky.service.stats;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.stats.SalesStatsResponse;
import org.sky.dto.stats.SellerAnalyticsResponse;
import org.sky.dto.stats.SellerStatsResponse;
import org.sky.dto.stats.AnalyticsSummaryResponse;
import org.sky.dto.stats.QuickSummaryResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.services.QuickSummaryService;
import org.sky.service.stats.calculators.AdminStatsCalculator;
import org.sky.service.stats.calculators.SellerStatsCalculator;
import org.sky.service.stats.calculators.AnalyticsCalculator;
import org.sky.service.stats.calculators.FinancialCalculator;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class StatsService {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    QuickSummaryService quickSummaryService;
    
    @Inject
    AdminStatsCalculator adminStatsCalculator;
    
    @Inject
    SellerStatsCalculator sellerStatsCalculator;
    
    @Inject
    AnalyticsCalculator analyticsCalculator;
    
    @Inject
    FinancialCalculator financialCalculator;
    
    private static final Logger log = Logger.getLogger(StatsService.class);
    
    /**
     * Obtiene estadísticas generales para un admin
     */
    @WithTransaction
    public Uni<SalesStatsResponse> getAdminStats(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 StatsService.getAdminStats() OPTIMIZED - AdminId: " + adminId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        // OPTIMIZACIÓN CRÍTICA: Límite máximo para evitar cargar millones de registros
        int maxDaysDiff = (int) startDate.until(endDate).getDays();
        if (maxDaysDiff > 30) {
            return Uni.createFrom().failure(new RuntimeException("Período muy amplio. Máximo 30 días permitidos para estadísticas."));
        }
        
        return adminStatsCalculator.calculateAdminStats(adminId, startDate, endDate);
    }
    
    /**
     * Obtiene estadísticas específicas para un vendedor
     */
    @WithTransaction
    public Uni<SellerStatsResponse> getSellerStats(Long sellerId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 StatsService.getSellerStats() - SellerId: " + sellerId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return sellerStatsCalculator.calculateSellerStats(sellerId, startDate, endDate);
    }
    
    /**
     * Obtiene resumen completo de analytics para admin
     */
    @WithTransaction
    public Uni<AnalyticsSummaryResponse> getAnalyticsSummary(Long adminId, LocalDate startDate, LocalDate endDate, 
                                                           String include, String period, String metric, 
                                                           String granularity, Double confidence, Integer days) {
        log.info("📊 StatsService.getAnalyticsSummary() - AdminId: " + adminId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return analyticsCalculator.calculateAdminAnalyticsSummary(
            adminId, startDate, endDate, include, period, metric, granularity, confidence, days);
    }
    
    /**
     * Obtiene resumen rápido para dashboard
     */
    @WithTransaction
    public Uni<QuickSummaryResponse> getQuickSummary(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 StatsService.getQuickSummary() - AdminId: " + adminId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return paymentNotificationRepository.findPaymentsForQuickSummary(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .map(payments -> {
                    log.info("📊 Calculando resumen rápido para " + payments.size() + " pagos");
                    
                    // Calcular métricas básicas
                    double totalSales = payments.stream()
                            .filter(p -> "CONFIRMED".equals(p.status))
                            .mapToDouble(p -> p.amount)
                            .sum();
                    
                    long totalTransactions = payments.size();
                    double averageTransactionValue = totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
                    
                    // Calcular métricas de estado
                    long pendingPayments = payments.stream().filter(p -> "PENDING".equals(p.status)).count();
                    long confirmedPayments = payments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
                    long rejectedPayments = payments.stream().filter(p -> "REJECTED_BY_SELLER".equals(p.status)).count();
                    
                    // Para este primer período, no hay datos históricos para comparar
                    // Por lo tanto el crecimiento será 0 hasta tener datos suficientes
                    double salesGrowth = 0.0;
                    double transactionGrowth = 0.0; 
                    double averageGrowth = 0.0;
                    
                    // Calcular tasa de reclamación
                    double claimRate = totalTransactions > 0 ? (double) confirmedPayments / totalTransactions * 100 : 0.0;
                    
                    // Tiempo promedio de confirmación calculado de datos reales
                    double averageConfirmationTime = calculateRealAvgConfirmationTime(payments);
                    
                    return new QuickSummaryResponse(
                        totalSales, totalTransactions, averageTransactionValue,
                        salesGrowth, transactionGrowth, averageGrowth,
                        pendingPayments, confirmedPayments, rejectedPayments,
                        claimRate, averageConfirmationTime
                    );
                });
    }
    
    /**
     * Obtiene analytics completos para un vendedor específico
     */
    @WithTransaction
    public Uni<SellerAnalyticsResponse> getSellerAnalyticsSummary(Long sellerId, LocalDate startDate, LocalDate endDate,
                                                                String include, String period, String metric,
                                                                String granularity, Double confidence, Integer days) {
        log.info("📊 StatsService.getSellerAnalyticsSummary() - SellerId: " + sellerId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return analyticsCalculator.calculateSellerAnalyticsSummary(
            sellerId, startDate, endDate, include, period, metric, granularity, confidence, days);
    }
    
    /**
     * Obtiene análisis financiero detallado para admin
     */
    @WithTransaction
    public Uni<Object> getFinancialAnalytics(Long adminId, LocalDate startDate, LocalDate endDate, 
                                            String include, String currency, Double taxRate) {
        log.info("💰 StatsService.getFinancialAnalytics() - AdminId: " + adminId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return financialCalculator.calculateFinancialAnalytics(adminId, startDate, endDate, include, currency, taxRate);
    }
    
    /**
     * Obtiene análisis financiero específico para vendedor
     */
    @WithTransaction
    public Uni<Object> getSellerFinancialAnalytics(Long sellerId, LocalDate startDate, LocalDate endDate,
                                                  String include, String currency, Double commissionRate) {
        log.info("💰 StatsService.getSellerFinancialAnalytics() - SellerId: " + sellerId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return financialCalculator.calculateSellerFinancialAnalytics(sellerId, startDate, endDate, include, currency, commissionRate);
    }
    
    /**
     * Obtiene reporte de transparencia de pagos
     */
    @WithTransaction
    public Uni<Object> getPaymentTransparencyReport(Long adminId, LocalDate startDate, LocalDate endDate,
                                                   Boolean includeFees, Boolean includeTaxes, Boolean includeCommissions) {
        log.info("🔍 StatsService.getPaymentTransparencyReport() - AdminId: " + adminId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return financialCalculator.calculatePaymentTransparencyReport(adminId, startDate, endDate, includeFees, includeTaxes, includeCommissions);
    }
    
    /**
     * Método utilitario para calcular tiempo promedio real de confirmación
     */
    private double calculateRealAvgConfirmationTime(List<PaymentNotification> payments) {
        if (payments == null || payments.isEmpty()) {
            return 0.0;
        }
        
        long totalMinutes = payments.stream()
                .filter(p -> p.confirmedAt != null && p.createdAt != null)
                .mapToLong(p -> java.time.Duration.between(p.createdAt, p.confirmedAt).toMinutes())
                .sum();
        
        long confirmedPayments = payments.stream()
                .filter(p -> p.confirmedAt != null)
                .count();
        
        return confirmedPayments > 0 ? (double) totalMinutes / confirmedPayments : 0.0;
    }
}
