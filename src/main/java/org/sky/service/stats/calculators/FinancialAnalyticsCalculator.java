package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.FinancialAnalyticsResponse;
import org.sky.model.PaymentNotification;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.calculators.template.BaseStatsCalculator;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.util.List;

@ApplicationScoped
public class FinancialAnalyticsCalculator extends BaseStatsCalculator<org.sky.service.stats.calculators.template.FinancialAnalyticsRequest, FinancialAnalyticsResponse> {

    private static final String DEFAULT_CURRENCY = "PEN";
    private static final double DEFAULT_TAX_RATE = 0.18;

    @Inject
    PaymentNotificationRepository paymentNotificationRepository;

    @Inject
    SellerRepository sellerRepository;

    @WithTransaction
    public Uni<FinancialAnalyticsResponse> calculateFinancialAnalytics(org.sky.dto.stats.FinancialAnalyticsRequest request) {
        var templateRequest = new org.sky.service.stats.calculators.template.FinancialAnalyticsRequest(
            request.adminId(), 
            request.startDate(), 
            request.endDate(),
            request.currency(),
            request.taxRate(),
            request.include()
        );
        
        return paymentNotificationRepository.findByAdminIdAndDateRange(
                request.adminId(), request.startDate().atStartOfDay(), request.endDate().atTime(23, 59, 59))
                .chain(payments -> sellerRepository.findByAdminId(request.adminId())
                        .map(sellers -> calculateStats(payments, templateRequest)));
    }

    @Override
    protected void validateInput(List<PaymentNotification> payments, org.sky.service.stats.calculators.template.FinancialAnalyticsRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        if (payments == null) {
            throw new IllegalArgumentException("Los pagos no pueden ser null");
        }
    }
    
    @Override
    protected List<PaymentNotification> filterPayments(List<PaymentNotification> payments, org.sky.service.stats.calculators.template.FinancialAnalyticsRequest request) {
        // Para financial analytics, filtramos solo pagos confirmados
        return payments.stream()
                .filter(payment -> "CONFIRMED".equals(payment.status))
                .toList();
    }
    
    @Override
    protected Object calculateSpecificMetrics(List<PaymentNotification> payments, org.sky.service.stats.calculators.template.FinancialAnalyticsRequest request) {
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
                                                     Object specificMetrics, List<PaymentNotification> payments, 
                                                     org.sky.service.stats.calculators.template.FinancialAnalyticsRequest request) {
        var financialMetrics = (FinancialSpecificMetrics) specificMetrics;
        
        var period = new FinancialAnalyticsResponse.Period(
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

