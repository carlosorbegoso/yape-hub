package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.stats.SalesStatsResponse;
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
public class AdminStatsCalculator {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    private static final Logger log = Logger.getLogger(AdminStatsCalculator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @WithTransaction
    public Uni<SalesStatsResponse> calculateAdminStats(Long adminId, LocalDate startDate, LocalDate endDate) {
        return paymentNotificationRepository.findPaymentsForStatsByAdminId(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .chain(payments -> {
                    // OPTIMIZACIÓN: Solo obtener sellers activos necesarios
                    return sellerRepository.find("branch.admin.id = ?1 and isActive = true", adminId)
                            .range(0, 100)  // LÍMITE: máximo 100 sellers
                            .list()
                            .map(sellers -> {
                                log.info("📊 OPTIMIZED - Procesando " + payments.size() + " pagos (límitado a 5000) para " + sellers.size() + " vendedores");
                                
                                // Calcular estadísticas generales
                                SalesStatsResponse.SummaryStats summary = calculateSummaryStats(payments);
                                
                                // Calcular estadísticas diarias
                                List<SalesStatsResponse.DailyStats> dailyStats = calculateDailyStats(payments, startDate, endDate);
                                
                                // Calcular estadísticas por vendedor
                                List<SalesStatsResponse.SellerStats> sellerStats = calculateSellerStats(payments, sellers);
                                
                                // Crear respuesta optimizada
                                SalesStatsResponse.PeriodInfo period = new SalesStatsResponse.PeriodInfo(
                                    startDate.format(DATE_FORMATTER),
                                    endDate.format(DATE_FORMATTER),
                                    (int) startDate.until(endDate).getDays() + 1
                                );
                                
                                return new SalesStatsResponse(period, summary, dailyStats, sellerStats);
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
}
