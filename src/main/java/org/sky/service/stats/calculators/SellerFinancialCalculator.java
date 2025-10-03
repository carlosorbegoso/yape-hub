package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.SellerFinancialResponse;
import org.sky.model.PaymentNotificationEntity;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.calculators.template.BaseStatsCalculator;
import org.sky.service.stats.calculators.template.SellerFinancialRequest;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class SellerFinancialCalculator extends BaseStatsCalculator<SellerFinancialRequest, SellerFinancialResponse> {
    
    private static final double DEFAULT_COMMISSION_RATE = 0.10;
    private static final String DEFAULT_CURRENCY = "PEN";
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @WithTransaction
    public Uni<SellerFinancialResponse> calculateSellerFinancialAnalytics(Long sellerId, LocalDate startDate, LocalDate endDate,
                                                        String include, String currency, Double commissionRate) {
        var request = new SellerFinancialRequest(sellerId, startDate, endDate, include, currency, commissionRate);
        
        return sellerRepository.findById(sellerId)
                .onItem().ifNull().failWith(() -> new RuntimeException("Seller not found"))
                .chain(seller -> paymentNotificationRepository.findByConfirmedByAndDateRange(
                        sellerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59))
                        .map(payments -> calculateStats(payments, request)));
    }
    
    @Override
    protected void validateInput(List<PaymentNotificationEntity> payments, SellerFinancialRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        if (payments == null) {
            throw new IllegalArgumentException("Los pagos no pueden ser null");
        }
    }
    
    @Override
    protected List<PaymentNotificationEntity> filterPayments(List<PaymentNotificationEntity> payments, SellerFinancialRequest request) {
        // Para seller financial, filtramos solo pagos confirmados del vendedor específico
        return payments.stream()
                .filter(payment -> "CONFIRMED".equals(payment.status))
                .filter(payment -> request.sellerId().equals(payment.confirmedBy))
                .toList();
    }
    
    @Override
    protected Object calculateSpecificMetrics(List<PaymentNotificationEntity> payments, SellerFinancialRequest request) {
        // Métricas específicas para seller financial: comisiones
        var totalSales = salesStrategy.calculate(payments);
        var finalCommissionRate = getCommissionRate(request.commissionRate());
        var commissionAmount = calculateCommission(totalSales, finalCommissionRate);
        var netAmount = totalSales - commissionAmount;
        
        return new SellerFinancialSpecificMetrics(
            totalSales,
            finalCommissionRate,
            commissionAmount,
            netAmount,
            getCurrency(request.currency())
        );
    }
    
    @Override
    protected SellerFinancialResponse buildResponse(Double totalSales, Long totalTransactions, 
                                                  Double averageTransactionValue, Double claimRate,
                                                  Object specificMetrics, List<PaymentNotificationEntity> payments,
                                                  SellerFinancialRequest request) {
        var sellerFinancialMetrics = (SellerFinancialSpecificMetrics) specificMetrics;
        
        // Para evitar bloqueo, usamos el ID del request directamente
        // En un entorno reactivo, la validación del seller se haría en el endpoint
        return new SellerFinancialResponse(
                request.sellerId(),
                sellerFinancialMetrics.totalSales(),
                sellerFinancialMetrics.commissionAmount(),
                sellerFinancialMetrics.netAmount(),
                totalTransactions.intValue(),
                averageTransactionValue,
                List.of(), // dailySales
                sellerFinancialMetrics.commissionRate(),
                calculateProfitMargin(sellerFinancialMetrics.totalSales(), sellerFinancialMetrics.commissionAmount()),
                new SellerFinancialResponse.Period(request.startDate().toString(), request.endDate().toString())
        );
    }
    
    private double getCommissionRate(Double commissionRate) {
        return commissionRate != null ? commissionRate : DEFAULT_COMMISSION_RATE;
    }
    
    private double calculateProfitMargin(Double totalSales, Double commissionAmount) {
        if (totalSales == null || totalSales == 0) return 0.0;
        return ((totalSales - commissionAmount) / totalSales) * 100.0;
    }
    
    private String getCurrency(String currency) {
        return currency != null ? currency : DEFAULT_CURRENCY;
    }
    
    /**
     * Métricas específicas para seller financial
     */
    private record SellerFinancialSpecificMetrics(
        Double totalSales,
        Double commissionRate,
        Double commissionAmount,
        Double netAmount,
        String currency
    ) {}
}
