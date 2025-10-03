package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.SellerStatsResponse;
import org.sky.model.PaymentNotificationEntity;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.calculators.template.BaseStatsCalculator;
import org.sky.service.stats.calculators.template.SellerStatsRequest;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class SellerStatsCalculator extends BaseStatsCalculator<SellerStatsRequest, SellerStatsResponse> {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @WithTransaction
    public Uni<SellerStatsResponse> calculateSellerStats(Long sellerId, LocalDate startDate, LocalDate endDate) {
        var request = new SellerStatsRequest(sellerId, startDate, endDate);
        
        return sellerRepository.findById(sellerId)
                .onItem().ifNull().failWith(() -> new RuntimeException("Vendedor no encontrado"))
                .chain(seller -> paymentNotificationRepository.findPaymentsForStatsByAdminId(
                        seller.branch.admin.id, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                        .map(payments -> calculateStats(payments, request)));
    }
    
    @Override
    protected void validateInput(List<PaymentNotificationEntity> payments, SellerStatsRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        if (payments == null) {
            throw new IllegalArgumentException("Los pagos no pueden ser null");
        }
    }
    
    @Override
    protected List<PaymentNotificationEntity> filterPayments(List<PaymentNotificationEntity> payments, SellerStatsRequest request) {
        // Filtrar pagos por vendedor específico
        return payments.stream()
                .filter(payment -> request.sellerId().equals(payment.confirmedBy))
                .toList();
    }
    
    @Override
    protected Object calculateSpecificMetrics(List<PaymentNotificationEntity> payments, SellerStatsRequest request) {
        // Métricas específicas para seller: estadísticas diarias
        return new SellerSpecificMetrics(
            calculateSellerDailyStats(payments, request.startDate(), request.endDate())
        );
    }
    
    @Override
    protected SellerStatsResponse buildResponse(Double totalSales, Long totalTransactions, 
                                              Double averageTransactionValue, Double claimRate,
                                              Object specificMetrics, List<PaymentNotificationEntity> payments,
                                              SellerStatsRequest request) {
        var sellerMetrics = (SellerSpecificMetrics) specificMetrics;
        
        // Para evitar bloqueo, calculamos claim rate basado en los pagos ya filtrados
        // En un entorno reactivo, la validación del seller se haría en el endpoint
        var claimRateForSeller = !payments.isEmpty() ? 
            (double) payments.size() / Math.max(payments.size(), 1) * 100 : 0.0;
        
        var summary = new SellerStatsResponse.SellerSummaryStats(
                totalSales, totalTransactions, averageTransactionValue,
                countPaymentsByStatus(payments, "PENDING"),
                countPaymentsByStatus(payments, "CONFIRMED"),
                countPaymentsByStatus(payments, "REJECTED_BY_SELLER"),
                claimRateForSeller
        );
        
        var period = createPeriodInfo(request.startDate(), request.endDate());
        
        return new SellerStatsResponse(
                request.sellerId(),
                "Seller " + request.sellerId(), // Nombre simplificado
                period,
                summary,
                sellerMetrics.dailyStats()
        );
    }
    
    private List<SellerStatsResponse.DailyStats> calculateSellerDailyStats(List<PaymentNotificationEntity> sellerPayments,
                                                                          LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> calculateDailyStatForDate(sellerPayments, date))
                .toList();
    }
    
    private SellerStatsResponse.DailyStats calculateDailyStatForDate(List<PaymentNotificationEntity> sellerPayments,
                                                                    LocalDate date) {
        var daySellerPayments = sellerPayments.stream()
                .filter(payment -> payment.createdAt.toLocalDate().equals(date))
                .toList();
        
        var daySales = daySellerPayments.stream()
                .filter(payment -> "CONFIRMED".equals(payment.status))
                .mapToDouble(payment -> payment.amount)
                .sum();
        
        var transactionCount = (long) daySellerPayments.size();
        var averageValue = transactionCount > 0 ? daySales / transactionCount : 0.0;
        var pendingCount = daySellerPayments.stream().filter(p -> "PENDING".equals(p.status)).count();
        var confirmedCount = daySellerPayments.stream().filter(p -> "CONFIRMED".equals(p.status)).count();
        
        return new SellerStatsResponse.DailyStats(
                date.format(DATE_FORMATTER),
                daySales,
                transactionCount,
                averageValue,
                pendingCount,
                confirmedCount
        );
    }
    
    private SellerStatsResponse.PeriodInfo createPeriodInfo(LocalDate startDate, LocalDate endDate) {
        int daysDiff = startDate.until(endDate).getDays();
        return new SellerStatsResponse.PeriodInfo(
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER),
                daysDiff + 1
        );
    }
    
    /**
     * Métricas específicas para seller
     */
    private record SellerSpecificMetrics(
        List<SellerStatsResponse.DailyStats> dailyStats
    ) {}
}