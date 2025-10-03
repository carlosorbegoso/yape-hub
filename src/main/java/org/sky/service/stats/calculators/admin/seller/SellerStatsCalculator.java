package org.sky.service.stats.calculators.admin.seller;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.SalesStatsResponse;
import org.sky.model.PaymentNotificationEntity;
import org.sky.model.SellerEntity;

import java.util.List;
@ApplicationScoped
public class SellerStatsCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String PENDING_STATUS = "PENDING";
    
    public List<SalesStatsResponse.SellerStats> calculateSellerStats(List<PaymentNotificationEntity> payments,
                                                                    List<SellerEntity> sellers) {
        return sellers.stream()
                .map(seller -> calculateSellerStat(payments, seller))
                .toList();
    }
    
    private SalesStatsResponse.SellerStats calculateSellerStat(List<PaymentNotificationEntity> payments, SellerEntity seller) {
        var sellerPayments = filterPaymentsBySeller(payments, seller.id);
        
        var sellerSales = calculateSellerSales(sellerPayments);
        var transactionCount = (long) sellerPayments.size();
        var averageValue = calculateAverageValue(sellerSales, transactionCount);
        var pendingCount = countPendingPayments(sellerPayments);
        
        return new SalesStatsResponse.SellerStats(
                seller.id,
                getSellerName(seller),
                sellerSales,
                transactionCount,
                averageValue,
                pendingCount
        );
    }
    
    private List<PaymentNotificationEntity> filterPaymentsBySeller(List<PaymentNotificationEntity> payments, Long sellerId) {
        return payments.stream()
                .filter(payment -> sellerId.equals(payment.confirmedBy))
                .toList();
    }
    
    private double calculateSellerSales(List<PaymentNotificationEntity> sellerPayments) {
        return sellerPayments.stream()
                .filter(payment -> CONFIRMED_STATUS.equals(payment.status))
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private double calculateAverageValue(double sellerSales, long transactionCount) {
        return transactionCount > 0 ? sellerSales / transactionCount : 0.0;
    }
    
    private long countPendingPayments(List<PaymentNotificationEntity> sellerPayments) {
        return sellerPayments.stream()
                .filter(payment -> PENDING_STATUS.equals(payment.status))
                .count();
    }
    
    private String getSellerName(SellerEntity seller) {
        return seller.sellerName != null ? seller.sellerName : "Sin nombre";
    }
}

