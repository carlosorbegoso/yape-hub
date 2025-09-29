package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.sky.dto.stats.SellerStatsResponse;
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
public class SellerStatsCalculator {
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    private static final Logger log = Logger.getLogger(SellerStatsCalculator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @WithTransaction
    public Uni<SellerStatsResponse> calculateSellerStats(Long sellerId, LocalDate startDate, LocalDate endDate) {
        return sellerRepository.findById(sellerId)
                .chain(seller -> {
                    if (seller == null) {
                        return Uni.createFrom().failure(new RuntimeException("Vendedor no encontrado"));
                    }
                    
                    // Obtener pagos del vendedor (todos los pagos del admin, pero filtrados por vendedor que los reclamó)
                    return paymentNotificationRepository.findPaymentsForStatsByAdminId(
                            seller.branch.admin.id, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
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
}
