package org.sky.service.stats.calculators.admin.summary;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.response.stats.SalesStatsResponse;
import org.sky.dto.response.stats.SummaryStats;
import org.sky.model.PaymentNotificationEntity;

import java.util.List;
@ApplicationScoped
public class SummaryStatsCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String PENDING_STATUS = "PENDING";
    private static final String REJECTED_BY_SELLER_STATUS = "REJECTED_BY_SELLER";
    
    public SummaryStats calculateSummaryStats(List<PaymentNotificationEntity> payments) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        
        var totalSales = calculateTotalSales(confirmedPayments);
        var totalTransactions = (long) payments.size();
        var averageTransactionValue = calculateAverageTransactionValue(totalSales, totalTransactions);
        var confirmedTransactions = (long) confirmedPayments.size();
        var pendingPayments = countPaymentsByStatus(payments, PENDING_STATUS);
        var rejectedPayments = countPaymentsByStatus(payments, REJECTED_BY_SELLER_STATUS);
        
        return new SummaryStats(
                totalSales, totalTransactions, averageTransactionValue, 
                pendingPayments, confirmedTransactions, rejectedPayments
        );
    }
    
    private List<PaymentNotificationEntity> filterPaymentsByStatus(List<PaymentNotificationEntity> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .toList();
    }
    
    private double calculateTotalSales(List<PaymentNotificationEntity> confirmedPayments) {
        return confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private double calculateAverageTransactionValue(double totalSales, long totalTransactions) {
        return totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
    }
    
    private long countPaymentsByStatus(List<PaymentNotificationEntity> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .count();
    }
}
