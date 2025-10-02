package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.QuickSummaryResponse;
import org.sky.model.PaymentNotification;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.service.stats.calculators.template.BaseStatsCalculator;
import org.sky.service.stats.calculators.template.AdminStatsRequest;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class QuickSummaryCalculator extends BaseStatsCalculator<AdminStatsRequest, QuickSummaryResponse> {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String PENDING_STATUS = "PENDING";
    private static final String REJECTED_STATUS = "REJECTED_BY_SELLER";
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @WithTransaction
    public Uni<QuickSummaryResponse> calculateQuickSummary(Long adminId, LocalDate startDate, LocalDate endDate) {
        var request = new AdminStatsRequest(adminId, startDate, endDate);
        
        return paymentNotificationRepository.findPaymentsForQuickSummary(
                adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                .map(payments -> calculateStats(payments, request));
    }
    
    @Override
    protected void validateInput(List<PaymentNotification> payments, AdminStatsRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        if (payments == null) {
            throw new IllegalArgumentException("Los pagos no pueden ser null");
        }
    }
    
    @Override
    protected List<PaymentNotification> filterPayments(List<PaymentNotification> payments, AdminStatsRequest request) {
        // Para quick summary, no filtramos
        return payments;
    }
    
    @Override
    protected Object calculateSpecificMetrics(List<PaymentNotification> payments, AdminStatsRequest request) {
        // Métricas específicas para quick summary: tiempos de confirmación
        var confirmedPayments = payments.stream()
                .filter(payment -> CONFIRMED_STATUS.equals(payment.status))
                .toList();
        
        var averageConfirmationTime = calculateAverageConfirmationTime(confirmedPayments);
        
        return new QuickSummarySpecificMetrics(
            averageConfirmationTime,
            countPaymentsByStatus(payments, PENDING_STATUS),
            countPaymentsByStatus(payments, CONFIRMED_STATUS),
            countPaymentsByStatus(payments, REJECTED_STATUS)
        );
    }
    
    @Override
    protected QuickSummaryResponse buildResponse(Double totalSales, Long totalTransactions, 
                                               Double averageTransactionValue, Double claimRate,
                                               Object specificMetrics, List<PaymentNotification> payments, 
                                               AdminStatsRequest request) {
        var quickMetrics = (QuickSummarySpecificMetrics) specificMetrics;
        
        return new QuickSummaryResponse(
                totalSales, totalTransactions, averageTransactionValue,
                0.0, 0.0, 0.0, // Growth metrics (no historical data available)
                quickMetrics.pendingCount(), quickMetrics.confirmedCount(), quickMetrics.rejectedCount(),
                claimRate, quickMetrics.averageConfirmationTime()
        );
    }
    
    private double calculateAverageConfirmationTime(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
                .filter(payment -> payment.confirmedAt != null)
                .mapToDouble(payment -> calculateConfirmationTimeInMinutes(payment.createdAt, payment.confirmedAt))
                .average()
                .orElse(0.0);
    }
    
    private double calculateConfirmationTimeInMinutes(LocalDateTime createdAt, LocalDateTime confirmedAt) {
        return Duration.between(createdAt, confirmedAt).toMinutes();
    }
    
    /**
     * Métricas específicas para quick summary
     */
    private record QuickSummarySpecificMetrics(
        Double averageConfirmationTime,
        Long pendingCount,
        Long confirmedCount,
        Long rejectedCount
    ) {}
}
