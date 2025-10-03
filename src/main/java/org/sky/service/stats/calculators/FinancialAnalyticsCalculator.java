package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.request.stats.FinancialAnalyticsRequest;
import org.sky.dto.response.common.Period;
import org.sky.dto.response.stats.FinancialAnalyticsResponse;
import org.sky.model.PaymentNotificationEntity;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.calculators.strategy.SalesCalculationStrategy;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.util.List;

@ApplicationScoped
public class FinancialAnalyticsCalculator extends BaseStatsCalculator<FinancialAnalyticsRequest, FinancialAnalyticsResponse> {

    private static final String DEFAULT_CURRENCY = "PEN";
    private static final double DEFAULT_TAX_RATE = 0.18;

    @Inject
    PaymentNotificationRepository paymentNotificationRepository;

    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    SalesCalculationStrategy salesStrategy;

    @WithTransaction
    public Uni<FinancialAnalyticsResponse> calculateFinancialAnalytics(FinancialAnalyticsRequest request) {
        
        return paymentNotificationRepository.findByAdminIdAndDateRange(
                request.adminId(), request.startDate().atStartOfDay(), request.endDate().atTime(23, 59, 59))
                .chain(payments -> sellerRepository.findByAdminId(request.adminId())
                        .map(sellers -> calculateStats(payments, request)));
    }

    @Override
    protected void validateInput(List<PaymentNotificationEntity> payments, FinancialAnalyticsRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        if (payments == null) {
            throw new IllegalArgumentException("Los pagos no pueden ser null");
        }
    }
    
    @Override
    protected List<PaymentNotificationEntity> filterPayments(List<PaymentNotificationEntity> payments,FinancialAnalyticsRequest request) {
        // Para financial analytics, filtramos solo pagos confirmados
        return payments.stream()
                .filter(payment -> "CONFIRMED".equals(payment.status))
                .toList();
    }
    
    @Override
    protected Object calculateSpecificMetrics(List<PaymentNotificationEntity> payments, FinancialAnalyticsRequest request) {
        // Métricas específicas para financial analytics: cálculos de impuestos
        var totalRevenue = salesStrategy.calculate(payments);
        var taxRate = getTaxRate(request.taxRate());
        var taxAmount = calculateTax(totalRevenue, taxRate);
        var netRevenue = totalRevenue - taxAmount;
        
        return new FinancialSpecificMetrics(
            totalRevenue,
            taxRate,
            taxAmount,
            netRevenue,
            getCurrency(request.currency())
        );
    }
    
    @Override
    protected FinancialAnalyticsResponse buildResponse(Double totalSales, Long totalTransactions, 
                                                     Double averageTransactionValue, Double claimRate,
                                                     Object specificMetrics, List<PaymentNotificationEntity> payments,
                                                     FinancialAnalyticsRequest request) {
        var financialMetrics = (FinancialSpecificMetrics) specificMetrics;
        
        var period = new Period(
                request.startDate().toString(),
                request.endDate().toString()
        );
        
        return new FinancialAnalyticsResponse(
                financialMetrics.totalRevenue(),
                financialMetrics.currency(),
                financialMetrics.taxRate(),
                financialMetrics.taxAmount(),
                financialMetrics.netRevenue(),
                period,
                request.include(),
                totalTransactions,
                countPaymentsByStatus(payments, "CONFIRMED"),
                averageTransactionValue
        );
    }
    
    private String getCurrency(String currency) {
        return currency != null ? currency : DEFAULT_CURRENCY;
    }
    
    private double getTaxRate(Double taxRate) {
        return taxRate != null ? taxRate : DEFAULT_TAX_RATE;
    }
    
    private double calculateTax(Double totalRevenue, double taxRate) {
        return totalRevenue * taxRate;
    }
    
    protected Long countPaymentsByStatus(List<PaymentNotificationEntity> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .count();
    }
    
    /**
     * Métricas específicas para financial analytics
     */
    private record FinancialSpecificMetrics(
        Double totalRevenue,
        Double taxRate,
        Double taxAmount,
        Double netRevenue,
        String currency
    ) {}
}

