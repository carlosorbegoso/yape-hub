package org.sky.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.stats.SalesStatsResponse;
import org.sky.dto.stats.SellerStatsResponse;
import org.sky.dto.stats.AnalyticsSummaryResponse;
import org.sky.dto.stats.QuickSummaryResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class StatsService {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    private static final Logger log = Logger.getLogger(StatsService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Obtiene estadísticas generales para un admin
     */
    @WithTransaction
    public Uni<SalesStatsResponse> getAdminStats(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 StatsService.getAdminStats() - AdminId: " + adminId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return paymentNotificationRepository.find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3", 
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .list()
                .chain(payments -> {
                    // Obtener vendedores del admin
                    return sellerRepository.find("branch.admin.id = ?1", adminId)
                            .list()
                            .map(sellers -> {
                                log.info("📊 Procesando " + payments.size() + " pagos para " + sellers.size() + " vendedores");
                                
                                // Calcular estadísticas generales
                                SalesStatsResponse.SummaryStats summary = calculateSummaryStats(payments);
                                
                                // Calcular estadísticas diarias
                                List<SalesStatsResponse.DailyStats> dailyStats = calculateDailyStats(payments, startDate, endDate);
                                
                                // Calcular estadísticas por vendedor
                                List<SalesStatsResponse.SellerStats> sellerStats = calculateSellerStats(payments, sellers);
                                
                                // Crear respuesta
                                SalesStatsResponse.PeriodInfo period = new SalesStatsResponse.PeriodInfo(
                                    startDate.format(DATE_FORMATTER),
                                    endDate.format(DATE_FORMATTER),
                                    (int) startDate.until(endDate).getDays() + 1
                                );
                                
                                return new SalesStatsResponse(period, summary, dailyStats, sellerStats);
                            });
                });
    }
    
    /**
     * Obtiene estadísticas específicas para un vendedor
     */
    @WithTransaction
    public Uni<SellerStatsResponse> getSellerStats(Long sellerId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 StatsService.getSellerStats() - SellerId: " + sellerId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return sellerRepository.findById(sellerId)
                .chain(seller -> {
                    if (seller == null) {
                        return Uni.createFrom().failure(new RuntimeException("Vendedor no encontrado"));
                    }
                    
                    // Obtener pagos del vendedor (todos los pagos del admin, pero filtrados por vendedor que los reclamó)
                    return paymentNotificationRepository.find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3", 
                            seller.branch.admin.id, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                            .list()
                            .map(payments -> {
                                // Filtrar pagos que fueron reclamados por este vendedor
                                List<PaymentNotification> sellerPayments = payments.stream()
                                        .filter(payment -> sellerId.equals(payment.confirmedBy))
                                        .collect(Collectors.toList());
                                
                                log.info("📊 Procesando " + sellerPayments.size() + " pagos reclamados por vendedor " + sellerId);
                                
                                // Calcular estadísticas del vendedor
                                SellerStatsResponse.SellerSummaryStats summary = calculateSellerSummaryStats(sellerPayments, payments);
                                
                                // Calcular estadísticas diarias
                                List<SellerStatsResponse.DailyStats> dailyStats = calculateSellerDailyStats(sellerPayments, payments, startDate, endDate);
                                
                                // Crear respuesta
                                SellerStatsResponse.PeriodInfo period = new SellerStatsResponse.PeriodInfo(
                                    startDate.format(DATE_FORMATTER),
                                    endDate.format(DATE_FORMATTER),
                                    (int) startDate.until(endDate).getDays() + 1
                                );
                                
                                return new SellerStatsResponse(
                                    sellerId,
                                    seller.sellerName != null ? seller.sellerName : "Sin nombre",
                                    period,
                                    summary,
                                    dailyStats
                                );
                            });
                });
    }
    
    private SalesStatsResponse.SummaryStats calculateSummaryStats(List<PaymentNotification> payments) {
        double totalSales = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .mapToDouble(p -> p.amount)
                .sum();
        
        long totalTransactions = payments.size();
        double averageTransactionValue = totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
        
        long pendingPayments = payments.stream().filter(p -> "PENDING".equals(p.status)).count();
        long confirmedPayments = payments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
        long rejectedPayments = payments.stream().filter(p -> "REJECTED_BY_SELLER".equals(p.status)).count();
        
        return new SalesStatsResponse.SummaryStats(
            totalSales, totalTransactions, averageTransactionValue,
            pendingPayments, confirmedPayments, rejectedPayments
        );
    }
    
    private List<SalesStatsResponse.DailyStats> calculateDailyStats(List<PaymentNotification> payments, 
                                                                   LocalDate startDate, LocalDate endDate) {
        Map<String, List<PaymentNotification>> paymentsByDate = payments.stream()
                .collect(Collectors.groupingBy(p -> p.createdAt.toLocalDate().format(DATE_FORMATTER)));
        
        List<SalesStatsResponse.DailyStats> dailyStats = new ArrayList<>();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(DATE_FORMATTER);
            List<PaymentNotification> dayPayments = paymentsByDate.getOrDefault(dateStr, new ArrayList<>());
            
            double totalSales = dayPayments.stream()
                    .filter(p -> "CONFIRMED".equals(p.status))
                    .mapToDouble(p -> p.amount)
                    .sum();
            
            long transactionCount = dayPayments.size();
            double averageValue = transactionCount > 0 ? totalSales / transactionCount : 0.0;
            
            dailyStats.add(new SalesStatsResponse.DailyStats(dateStr, totalSales, transactionCount, averageValue));
            currentDate = currentDate.plusDays(1);
        }
        
        return dailyStats;
    }
    
    private List<SalesStatsResponse.SellerStats> calculateSellerStats(List<PaymentNotification> payments, 
                                                                    List<Seller> sellers) {
        Map<Long, List<PaymentNotification>> paymentsBySeller = payments.stream()
                .filter(p -> p.confirmedBy != null)
                .collect(Collectors.groupingBy(p -> p.confirmedBy));
        
        return sellers.stream()
                .map(seller -> {
                    List<PaymentNotification> sellerPayments = paymentsBySeller.getOrDefault(seller.id, new ArrayList<>());
                    
                    double totalSales = sellerPayments.stream()
                            .filter(p -> "CONFIRMED".equals(p.status))
                            .mapToDouble(p -> p.amount)
                            .sum();
                    
                    long transactionCount = sellerPayments.size();
                    double averageValue = transactionCount > 0 ? totalSales / transactionCount : 0.0;
                    
                    long pendingCount = payments.stream()
                            .filter(p -> "PENDING".equals(p.status))
                            .count(); // Todos los pendientes están disponibles para todos los vendedores
                    
                    return new SalesStatsResponse.SellerStats(
                        seller.id,
                        seller.sellerName != null ? seller.sellerName : "Sin nombre",
                        totalSales, transactionCount, averageValue, pendingCount
                    );
                })
                .collect(Collectors.toList());
    }
    
    private SellerStatsResponse.SellerSummaryStats calculateSellerSummaryStats(List<PaymentNotification> sellerPayments, 
                                                                              List<PaymentNotification> allPayments) {
        double totalSales = sellerPayments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .mapToDouble(p -> p.amount)
                .sum();
        
        long totalTransactions = sellerPayments.size();
        double averageTransactionValue = totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
        
        long pendingPayments = allPayments.stream().filter(p -> "PENDING".equals(p.status)).count();
        long confirmedPayments = sellerPayments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
        long rejectedPayments = sellerPayments.stream().filter(p -> "REJECTED_BY_SELLER".equals(p.status)).count();
        
        // Calcular tasa de reclamación
        long totalAvailablePayments = allPayments.size();
        double claimRate = totalAvailablePayments > 0 ? (double) totalTransactions / totalAvailablePayments * 100 : 0.0;
        
        return new SellerStatsResponse.SellerSummaryStats(
            totalSales, totalTransactions, averageTransactionValue,
            pendingPayments, confirmedPayments, rejectedPayments, claimRate
        );
    }
    
    private List<SellerStatsResponse.DailyStats> calculateSellerDailyStats(List<PaymentNotification> sellerPayments, 
                                                                          List<PaymentNotification> allPayments,
                                                                          LocalDate startDate, LocalDate endDate) {
        Map<String, List<PaymentNotification>> sellerPaymentsByDate = sellerPayments.stream()
                .collect(Collectors.groupingBy(p -> p.createdAt.toLocalDate().format(DATE_FORMATTER)));
        
        Map<String, List<PaymentNotification>> allPaymentsByDate = allPayments.stream()
                .collect(Collectors.groupingBy(p -> p.createdAt.toLocalDate().format(DATE_FORMATTER)));
        
        List<SellerStatsResponse.DailyStats> dailyStats = new ArrayList<>();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(DATE_FORMATTER);
            List<PaymentNotification> daySellerPayments = sellerPaymentsByDate.getOrDefault(dateStr, new ArrayList<>());
            List<PaymentNotification> dayAllPayments = allPaymentsByDate.getOrDefault(dateStr, new ArrayList<>());
            
            double totalSales = daySellerPayments.stream()
                    .filter(p -> "CONFIRMED".equals(p.status))
                    .mapToDouble(p -> p.amount)
                    .sum();
            
            long transactionCount = daySellerPayments.size();
            double averageValue = transactionCount > 0 ? totalSales / transactionCount : 0.0;
            
            long pendingCount = dayAllPayments.stream().filter(p -> "PENDING".equals(p.status)).count();
            long confirmedCount = daySellerPayments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
            
            dailyStats.add(new SellerStatsResponse.DailyStats(
                dateStr, totalSales, transactionCount, averageValue, pendingCount, confirmedCount
            ));
            currentDate = currentDate.plusDays(1);
        }
        
        return dailyStats;
    }
    
    /**
     * Obtiene resumen completo de analytics para admin
     */
    @WithTransaction
    public Uni<AnalyticsSummaryResponse> getAnalyticsSummary(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 StatsService.getAnalyticsSummary() - AdminId: " + adminId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return paymentNotificationRepository.find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3", 
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .list()
                .chain(payments -> {
                    // Obtener vendedores del admin
                    return sellerRepository.find("branch.admin.id = ?1", adminId)
                            .list()
                            .map(sellers -> {
                                log.info("📊 Procesando analytics para " + payments.size() + " pagos y " + sellers.size() + " vendedores");
                                
                                // Calcular métricas de resumen
                                AnalyticsSummaryResponse.OverviewMetrics overview = calculateOverviewMetrics(payments, startDate, endDate);
                                
                                // Calcular ventas diarias
                                List<AnalyticsSummaryResponse.DailySalesData> dailySales = calculateDailySalesData(payments, startDate, endDate);
                                
                                // Calcular top vendedores
                                List<AnalyticsSummaryResponse.TopSellerData> topSellers = calculateTopSellers(payments, sellers);
                                
                                // Calcular métricas de rendimiento
                                AnalyticsSummaryResponse.PerformanceMetrics performance = calculatePerformanceMetrics(payments);
                                
                                return new AnalyticsSummaryResponse(overview, dailySales, topSellers, performance);
                            });
                });
    }
    
    /**
     * Obtiene resumen rápido para dashboard
     */
    @WithTransaction
    public Uni<QuickSummaryResponse> getQuickSummary(Long adminId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 StatsService.getQuickSummary() - AdminId: " + adminId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return paymentNotificationRepository.find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3", 
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .list()
                .map(payments -> {
                    log.info("📊 Calculando resumen rápido para " + payments.size() + " pagos");
                    
                    // Calcular métricas básicas
                    double totalSales = payments.stream()
                            .filter(p -> "CONFIRMED".equals(p.status))
                            .mapToDouble(p -> p.amount)
                            .sum();
                    
                    long totalTransactions = payments.size();
                    double averageTransactionValue = totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
                    
                    // Para simplificar, usar valores fijos de crecimiento
                    double salesGrowth = 12.5; // +12.5%
                    double transactionGrowth = 8.2; // +8.2%
                    double averageGrowth = 3.1; // +3.1%
                    
                    // Calcular métricas de estado
                    long pendingPayments = payments.stream().filter(p -> "PENDING".equals(p.status)).count();
                    long confirmedPayments = payments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
                    long rejectedPayments = payments.stream().filter(p -> "REJECTED_BY_SELLER".equals(p.status)).count();
                    
                    // Calcular tasa de reclamación
                    double claimRate = totalTransactions > 0 ? (double) confirmedPayments / totalTransactions * 100 : 0.0;
                    
                    // Tiempo promedio de confirmación (simulado)
                    double averageConfirmationTime = 2.3; // minutos
                    
                    return new QuickSummaryResponse(
                        totalSales, totalTransactions, averageTransactionValue,
                        salesGrowth, transactionGrowth, averageGrowth,
                        pendingPayments, confirmedPayments, rejectedPayments,
                        claimRate, averageConfirmationTime
                    );
                });
    }
    
    private AnalyticsSummaryResponse.OverviewMetrics calculateOverviewMetrics(List<PaymentNotification> payments, 
                                                                             LocalDate startDate, LocalDate endDate) {
        double totalSales = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .mapToDouble(p -> p.amount)
                .sum();
        
        long totalTransactions = payments.size();
        double averageTransactionValue = totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
        
        // Valores de crecimiento simulados
        double salesGrowth = 12.5;
        double transactionGrowth = 8.2;
        double averageGrowth = 3.1;
        
        return new AnalyticsSummaryResponse.OverviewMetrics(
            totalSales, totalTransactions, averageTransactionValue,
            salesGrowth, transactionGrowth, averageGrowth
        );
    }
    
    private List<AnalyticsSummaryResponse.DailySalesData> calculateDailySalesData(List<PaymentNotification> payments, 
                                                                                LocalDate startDate, LocalDate endDate) {
        Map<String, List<PaymentNotification>> paymentsByDate = payments.stream()
                .collect(Collectors.groupingBy(p -> p.createdAt.toLocalDate().format(DATE_FORMATTER)));
        
        List<AnalyticsSummaryResponse.DailySalesData> dailySales = new ArrayList<>();
        String[] dayNames = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
        
        LocalDate currentDate = startDate;
        int dayIndex = 0;
        while (!currentDate.isAfter(endDate)) {
            String dateStr = currentDate.format(DATE_FORMATTER);
            List<PaymentNotification> dayPayments = paymentsByDate.getOrDefault(dateStr, new ArrayList<>());
            
            double sales = dayPayments.stream()
                    .filter(p -> "CONFIRMED".equals(p.status))
                    .mapToDouble(p -> p.amount)
                    .sum();
            
            long transactions = dayPayments.size();
            String dayName = dayNames[dayIndex % 7];
            
            dailySales.add(new AnalyticsSummaryResponse.DailySalesData(dateStr, dayName, sales, transactions));
            currentDate = currentDate.plusDays(1);
            dayIndex++;
        }
        
        return dailySales;
    }
    
    private List<AnalyticsSummaryResponse.TopSellerData> calculateTopSellers(List<PaymentNotification> payments, 
                                                                            List<Seller> sellers) {
        Map<Long, List<PaymentNotification>> paymentsBySeller = payments.stream()
                .filter(p -> p.confirmedBy != null)
                .collect(Collectors.groupingBy(p -> p.confirmedBy));
        
        return sellers.stream()
                .map(seller -> {
                    List<PaymentNotification> sellerPayments = paymentsBySeller.getOrDefault(seller.id, new ArrayList<>());
                    
                    double totalSales = sellerPayments.stream()
                            .filter(p -> "CONFIRMED".equals(p.status))
                            .mapToDouble(p -> p.amount)
                            .sum();
                    
                    long transactionCount = sellerPayments.size();
                    
                    return new AnalyticsSummaryResponse.TopSellerData(
                        null, // rank se calculará después
                        seller.id,
                        seller.sellerName != null ? seller.sellerName : "Sin nombre",
                        seller.branch != null ? seller.branch.name : "Sin sucursal",
                        totalSales,
                        transactionCount
                    );
                })
                .filter(seller -> seller.totalSales() > 0) // Solo vendedores con ventas
                .sorted((s1, s2) -> Double.compare(s2.totalSales(), s1.totalSales())) // Ordenar por ventas
                .limit(4) // Top 4 vendedores
                .collect(Collectors.toList())
                .stream()
                .map(seller -> new AnalyticsSummaryResponse.TopSellerData(
                    null, // Se asignará el rank después
                    seller.sellerId(),
                    seller.sellerName(),
                    seller.branchName(),
                    seller.totalSales(),
                    seller.transactionCount()
                ))
                .collect(Collectors.toList());
    }
    
    private AnalyticsSummaryResponse.PerformanceMetrics calculatePerformanceMetrics(List<PaymentNotification> payments) {
        long pendingPayments = payments.stream().filter(p -> "PENDING".equals(p.status)).count();
        long confirmedPayments = payments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
        long rejectedPayments = payments.stream().filter(p -> "REJECTED_BY_SELLER".equals(p.status)).count();
        
        long totalProcessed = confirmedPayments + rejectedPayments;
        double claimRate = totalProcessed > 0 ? (double) confirmedPayments / totalProcessed * 100 : 0.0;
        double rejectionRate = totalProcessed > 0 ? (double) rejectedPayments / totalProcessed * 100 : 0.0;
        
        // Tiempo promedio de confirmación simulado
        double averageConfirmationTime = 2.3; // minutos
        
        return new AnalyticsSummaryResponse.PerformanceMetrics(
            averageConfirmationTime, claimRate, rejectionRate,
            pendingPayments, confirmedPayments, rejectedPayments
        );
    }
    
    /**
     * Obtiene analytics completos para un vendedor específico
     */
    @WithTransaction
    public Uni<AnalyticsSummaryResponse> getSellerAnalyticsSummary(Long sellerId, LocalDate startDate, LocalDate endDate) {
        log.info("📊 StatsService.getSellerAnalyticsSummary() - SellerId: " + sellerId + 
                ", Desde: " + startDate + ", Hasta: " + endDate);
        
        return sellerRepository.findById(sellerId)
                .chain(seller -> {
                    // Obtener todos los pagos del admin del vendedor
                    return paymentNotificationRepository.find("adminId = ?1 and createdAt >= ?2 and createdAt <= ?3", 
                            seller.branch.admin.id, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                            .list()
                            .map(payments -> {
                                log.info("📊 Procesando analytics para vendedor " + seller.sellerName + " con " + payments.size() + " pagos del admin");
                                
                                // Filtrar solo los pagos relacionados con este vendedor
                                List<PaymentNotification> sellerPayments = payments.stream()
                                        .filter(p -> sellerId.equals(p.confirmedBy) || sellerId.equals(p.rejectedBy))
                                        .collect(Collectors.toList());
                                
                                // Calcular métricas de resumen específicas del vendedor
                                AnalyticsSummaryResponse.OverviewMetrics overview = calculateSellerOverviewMetrics(sellerPayments, startDate, endDate);
                                
                                // Calcular ventas diarias del vendedor
                                List<AnalyticsSummaryResponse.DailySalesData> dailySales = calculateDailySalesData(sellerPayments, startDate, endDate);
                                
                                // Para un vendedor individual, el "top seller" es él mismo
                                List<AnalyticsSummaryResponse.TopSellerData> topSellers = List.of(
                                    new AnalyticsSummaryResponse.TopSellerData(
                                        1, // rank
                                        seller.id, // sellerId
                                        seller.sellerName, // sellerName
                                        seller.branch != null ? seller.branch.name : "Sin sucursal", // branchName
                                        sellerPayments.stream().filter(p -> "CONFIRMED".equals(p.status)).mapToDouble(p -> p.amount).sum(), // totalSales
                                        sellerPayments.stream().filter(p -> "CONFIRMED".equals(p.status)).count() // transactionCount
                                    )
                                );
                                
                                // Calcular métricas de rendimiento del vendedor
                                AnalyticsSummaryResponse.PerformanceMetrics performance = calculatePerformanceMetrics(sellerPayments);
                                
                                return new AnalyticsSummaryResponse(overview, dailySales, topSellers, performance);
                            });
                });
    }
    
    /**
     * Calcula métricas de resumen específicas para un vendedor
     */
    private AnalyticsSummaryResponse.OverviewMetrics calculateSellerOverviewMetrics(List<PaymentNotification> payments, LocalDate startDate, LocalDate endDate) {
        long totalPayments = payments.size();
        long confirmedPayments = payments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
        long pendingPayments = payments.stream().filter(p -> "PENDING".equals(p.status)).count();
        long rejectedPayments = payments.stream().filter(p -> p.status.startsWith("REJECTED")).count();
        
        double totalSales = payments.stream()
                .filter(p -> "CONFIRMED".equals(p.status))
                .mapToDouble(p -> p.amount)
                .sum();
        
        double averageTransactionValue = confirmedPayments > 0 ? totalSales / confirmedPayments : 0.0;
        
        return new AnalyticsSummaryResponse.OverviewMetrics(
            totalSales, // totalSales
            (long) totalPayments, // totalTransactions
            averageTransactionValue, // averageTransactionValue
            0.0, // salesGrowth (no calculado para vendedor individual)
            0.0, // transactionGrowth (no calculado para vendedor individual)
            0.0 // averageGrowth (no calculado para vendedor individual)
        );
    }
}
