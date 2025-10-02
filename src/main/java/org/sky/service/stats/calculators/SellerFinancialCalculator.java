package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.SellerFinancialResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class SellerFinancialCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final double DEFAULT_COMMISSION_RATE = 0.10;
    private static final String DEFAULT_CURRENCY = "PEN";
    private static final String DEFAULT_SELLER_NAME = "Sin nombre";
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @WithTransaction
    public Uni<SellerFinancialResponse> calculateSellerFinancialAnalytics(Long sellerId, LocalDate startDate, LocalDate endDate,
                                                        String include, String currency, Double commissionRate) {
        return sellerRepository.findById(sellerId)
                .onItem().ifNull().failWith(() -> new RuntimeException("Seller not found"))
                .chain(seller -> paymentNotificationRepository.findByConfirmedByAndDateRange(
                        sellerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                        .map(payments -> buildFinancialResponse(seller, payments, startDate, endDate, 
                                include, currency, commissionRate)));
    }
    
    private SellerFinancialResponse buildFinancialResponse(Seller seller, List<PaymentNotification> payments,
                                                          LocalDate startDate, LocalDate endDate,
                                                          String include, String currency, Double commissionRate) {
        var confirmedPayments = payments.stream()
                .filter(payment -> CONFIRMED_STATUS.equals(payment.status))
                .toList();
        
        var totalSales = calculateTotalSales(confirmedPayments);
        var finalCommissionRate = getCommissionRate(commissionRate);
        var commissionAmount = totalSales * finalCommissionRate;
        
        return new SellerFinancialResponse(
                seller.id,
                getSellerName(seller),
                totalSales,
                getCurrency(currency),
                finalCommissionRate,
                commissionAmount,
                totalSales - commissionAmount,
                new SellerFinancialResponse.Period(startDate.toString(), endDate.toString()),
                include,
                payments.size(),
                (long) confirmedPayments.size(),
                calculateAverageTransactionValue(payments)
        );
    }
    
    private double calculateTotalSales(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private double calculateAverageTransactionValue(List<PaymentNotification> payments) {
        return payments.stream()
                .mapToDouble(payment -> payment.amount)
                .average()
                .orElse(0.0);
    }
    
    private double getCommissionRate(Double commissionRate) {
        return commissionRate != null ? commissionRate : DEFAULT_COMMISSION_RATE;
    }
    
    private String getCurrency(String currency) {
        return currency != null ? currency : DEFAULT_CURRENCY;
    }
    
    private String getSellerName(Seller seller) {
        return seller.sellerName != null ? seller.sellerName : DEFAULT_SELLER_NAME;
    }
}
