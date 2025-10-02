package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.AdminStatsResponse;
import org.sky.model.PaymentNotification;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.calculators.template.BaseStatsCalculator;
import org.sky.service.stats.calculators.template.AdminStatsRequest;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class AdminStatsCalculator extends BaseStatsCalculator<AdminStatsRequest, AdminStatsResponse> {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @WithTransaction
    public Uni<AdminStatsResponse> calculateAdminStats(Long adminId, LocalDate startDate, LocalDate endDate) {
        var request = new AdminStatsRequest(adminId, startDate, endDate);
        
        return paymentNotificationRepository.findPaymentsForStatsByAdminId(
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
        return payments.stream()
                .filter(payment -> payment.adminId.equals(request.adminId()))
                .toList();
    }
    
    @Override
    protected Object calculateSpecificMetrics(List<PaymentNotification> payments, AdminStatsRequest request) {
        return null; // No specific metrics for admin stats
    }
    
    @Override
    protected AdminStatsResponse buildResponse(Double totalSales, Long totalTransactions, 
                                             Double averageTransactionValue, Double claimRate,
                                             Object specificMetrics, List<PaymentNotification> payments, AdminStatsRequest request) {
        return new AdminStatsResponse(
            request.adminId(),
            totalSales,
            totalTransactions,
            averageTransactionValue,
            request.startDate(),
            request.endDate()
        );
    }
}