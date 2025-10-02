package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.SellerStatsResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class SellerStatsCalculator {
    
    private static final String DEFAULT_SELLER_NAME = "Sin nombre";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @WithTransaction
    public Uni<SellerStatsResponse> calculateSellerStats(Long sellerId, LocalDate startDate, LocalDate endDate) {
        return sellerRepository.findById(sellerId)
                .onItem().ifNull().failWith(() -> new RuntimeException("Vendedor no encontrado"))
                .chain(seller -> paymentNotificationRepository.findPaymentsForStatsByAdminId(
                        seller.branch.admin.id, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                        .map(payments -> buildSellerStatsResponse(seller, payments, startDate, endDate)));
    }
    
    private SellerStatsResponse buildSellerStatsResponse(Seller seller, List<PaymentNotification> allPayments,
                                                        LocalDate startDate, LocalDate endDate) {
        var sellerPayments = filterPaymentsBySeller(allPayments, seller.id);
        var summary = calculateSellerSummaryStats(sellerPayments, allPayments);
        var dailyStats = calculateSellerDailyStats(sellerPayments, allPayments, startDate, endDate);
        var period = createPeriodInfo(startDate, endDate);
        
        return new SellerStatsResponse(
                seller.id,
                getSellerName(seller),
                period,
                summary,
                dailyStats
        );
    }
    
    private List<PaymentNotification> filterPaymentsBySeller(List<PaymentNotification> payments, Long sellerId) {
        return payments.stream()
                .filter(payment -> sellerId.equals(payment.confirmedBy))
                .toList();
    }
    
    private SellerStatsResponse.SellerSummaryStats calculateSellerSummaryStats(List<PaymentNotification> sellerPayments,
                                                                              List<PaymentNotification> allPayments) {
        var confirmedPayments = sellerPayments.stream()
                .filter(payment -> "CONFIRMED".equals(payment.status))
                .toList();
        
        var totalSales = confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
        
        var totalTransactions = (long) sellerPayments.size();
        var averageTransactionValue = totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
        var pendingPayments = allPayments.stream().filter(p -> "PENDING".equals(p.status)).count();
        var confirmedTransactions = (long) confirmedPayments.size();
        var rejectedPayments = sellerPayments.stream().filter(p -> "REJECTED_BY_SELLER".equals(p.status)).count();
        var claimRate = !allPayments.isEmpty() ? (double) sellerPayments.size() / allPayments.size() * 100 : 0.0;
        
        return new SellerStatsResponse.SellerSummaryStats(
                totalSales, totalTransactions, averageTransactionValue,
                pendingPayments, confirmedTransactions, rejectedPayments, claimRate
        );
    }
    
    private List<SellerStatsResponse.DailyStats> calculateSellerDailyStats(List<PaymentNotification> sellerPayments,
                                                                          List<PaymentNotification> allPayments,
                                                                          LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> calculateDailyStatForDate(sellerPayments, allPayments, date))
                .toList();
    }
    
    private SellerStatsResponse.DailyStats calculateDailyStatForDate(List<PaymentNotification> sellerPayments,
                                                                    List<PaymentNotification> allPayments,
                                                                    LocalDate date) {
        var daySellerPayments = sellerPayments.stream()
                .filter(payment -> payment.createdAt.toLocalDate().equals(date))
                .toList();
        
        var dayAllPayments = allPayments.stream()
                .filter(payment -> payment.createdAt.toLocalDate().equals(date))
                .toList();
        
        var daySales = daySellerPayments.stream()
                .filter(payment -> "CONFIRMED".equals(payment.status))
                .mapToDouble(payment -> payment.amount)
                .sum();
        
        var transactionCount = (long) daySellerPayments.size();
        var averageValue = transactionCount > 0 ? daySales / transactionCount : 0.0;
        var pendingCount = dayAllPayments.stream().filter(p -> "PENDING".equals(p.status)).count();
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
    
    private String getSellerName(Seller seller) {
        return seller.sellerName != null ? seller.sellerName : DEFAULT_SELLER_NAME;
    }
}