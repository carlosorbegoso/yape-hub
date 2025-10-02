package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.QuickSummaryResponse;
import org.sky.model.PaymentNotification;
import org.sky.repository.PaymentNotificationRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class QuickSummaryCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String PENDING_STATUS = "PENDING";
    private static final String REJECTED_STATUS = "REJECTED_BY_SELLER";
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @WithTransaction
    public Uni<QuickSummaryResponse> calculateQuickSummary(Long adminId, LocalDate startDate, LocalDate endDate) {
        return paymentNotificationRepository.findPaymentsForQuickSummary(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .map(this::buildQuickSummaryResponse);
    }
    
    private QuickSummaryResponse buildQuickSummaryResponse(List<PaymentNotification> payments) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        var pendingPayments = filterPaymentsByStatus(payments, PENDING_STATUS);
        var rejectedPayments = filterPaymentsByStatus(payments, REJECTED_STATUS);
        
        var totalSales = calculateTotalSales(confirmedPayments);
        var totalTransactions = payments.size();
        var averageTransactionValue = calculateAverageTransactionValue(totalSales, totalTransactions);
        var claimRate = calculateClaimRate(confirmedPayments.size(), totalTransactions);
        var averageConfirmationTime = calculateAverageConfirmationTime(confirmedPayments);
        
        return new QuickSummaryResponse(
                totalSales, (long) totalTransactions, averageTransactionValue,
                0.0, 0.0, 0.0, // Growth metrics (no historical data available)
                (long) pendingPayments.size(), (long) confirmedPayments.size(), (long) rejectedPayments.size(),
                claimRate, averageConfirmationTime
        );
    }
    
    private List<PaymentNotification> filterPaymentsByStatus(List<PaymentNotification> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .toList();
    }
    
    private double calculateTotalSales(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private double calculateAverageTransactionValue(double totalSales, int totalTransactions) {
        return totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
    }
    
    private double calculateClaimRate(int confirmedCount, int totalCount) {
        return totalCount > 0 ? (double) confirmedCount / totalCount * 100 : 0.0;
    }
    
    private double calculateAverageConfirmationTime(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
                .filter(payment -> payment.confirmedAt != null)
                .mapToDouble(payment -> calculateConfirmationTimeInMinutes(payment.createdAt, payment.confirmedAt))
                .average()
                .orElse(0.0);
    }
    
    private double calculateConfirmationTimeInMinutes(LocalDateTime createdAt, LocalDateTime confirmedAt) {
        return java.time.Duration.between(createdAt, confirmedAt).toMinutes();
    }
}
