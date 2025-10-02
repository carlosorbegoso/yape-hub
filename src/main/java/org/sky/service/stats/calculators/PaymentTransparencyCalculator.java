package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.PaymentTransparencyRequest;
import org.sky.dto.stats.PaymentTransparencyResponse;
import org.sky.model.PaymentNotification;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.calculators.template.BaseStatsCalculator;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class PaymentTransparencyCalculator extends BaseStatsCalculator<org.sky.service.stats.calculators.template.PaymentTransparencyRequest, PaymentTransparencyResponse> {

    private static final double PROCESSING_FEE_RATE = 0.02; // 2%
    private static final double PLATFORM_FEE_RATE = 0.01; // 1%
    private static final double TAX_RATE = 0.18; // 18% IGV
    private static final double SELLER_COMMISSION_RATE = 0.10; // 10%
    private static final double TRANSPARENCY_SCORE = 95.0;

    @Inject
    PaymentNotificationRepository paymentNotificationRepository;

    @Inject
    SellerRepository sellerRepository;

    @WithTransaction
    public Uni<PaymentTransparencyResponse> calculatePaymentTransparencyReport(PaymentTransparencyRequest request) {
        var templateRequest = new org.sky.service.stats.calculators.template.PaymentTransparencyRequest(
            request.adminId(),
            request.startDate(),
            request.endDate(),
            request.includeFees(),
            request.includeTaxes(),
            request.includeCommissions()
        );
        
        return paymentNotificationRepository.findByAdminIdAndDateRange(
                request.adminId(), request.startDate().atStartOfDay(), request.endDate().atTime(23, 59, 59))
                .chain(payments -> sellerRepository.findByAdminId(request.adminId())
                        .map(sellers -> calculateStats(payments, templateRequest)));
    }

    @Override
    protected void validateInput(List<PaymentNotification> payments, org.sky.service.stats.calculators.template.PaymentTransparencyRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        if (payments == null) {
            throw new IllegalArgumentException("Los pagos no pueden ser null");
        }
    }
    
    @Override
    protected List<PaymentNotification> filterPayments(List<PaymentNotification> payments, org.sky.service.stats.calculators.template.PaymentTransparencyRequest request) {
        // Para transparency, filtramos solo pagos confirmados
        return payments.stream()
                .filter(payment -> "CONFIRMED".equals(payment.status))
                .toList();
    }
    
    @Override
    protected Object calculateSpecificMetrics(List<PaymentNotification> payments, org.sky.service.stats.calculators.template.PaymentTransparencyRequest request) {
        // Métricas específicas para transparency: fees, taxes, comisiones
        var totalRevenue = salesStrategy.calculate(payments);
        
        var processingFees = shouldIncludeFees(request.includeFees()) ? 
                calculateFee(totalRevenue, PROCESSING_FEE_RATE) : null;
        var platformFees = shouldIncludeFees(request.includeFees()) ? 
                calculateFee(totalRevenue, PLATFORM_FEE_RATE) : null;
        
        var taxRate = shouldIncludeTaxes(request.includeTaxes()) ? TAX_RATE : null;
        var taxAmount = shouldIncludeTaxes(request.includeTaxes()) ? 
                calculateTax(totalRevenue, TAX_RATE) : null;
        
        var sellerCommissionRate = shouldIncludeCommissions(request.includeCommissions()) ? 
                SELLER_COMMISSION_RATE : null;
        var sellerCommissionAmount = shouldIncludeCommissions(request.includeCommissions()) ? 
                calculateCommission(totalRevenue, SELLER_COMMISSION_RATE) : null;
        
        var transparencyScore = calculateTransparencyScore(request);
        
        return new TransparencySpecificMetrics(
            processingFees,
            platformFees,
            taxRate,
            taxAmount,
            sellerCommissionRate,
            sellerCommissionAmount,
            transparencyScore
        );
    }
    
    @Override
    protected PaymentTransparencyResponse buildResponse(Double totalSales, Long totalTransactions, 
                                                      Double averageTransactionValue, Double claimRate,
                                                      Object specificMetrics, List<PaymentNotification> payments, 
                                                      org.sky.service.stats.calculators.template.PaymentTransparencyRequest request) {
        var transparencyMetrics = (TransparencySpecificMetrics) specificMetrics;
        
        var period = new PaymentTransparencyResponse.Period(
                request.startDate().toString(),
                request.endDate().toString()
        );
        
        return new PaymentTransparencyResponse(
                period, totalSales, totalTransactions, countPaymentsByStatus(payments, "CONFIRMED"),
                transparencyMetrics.processingFees(), transparencyMetrics.platformFees(),
                transparencyMetrics.taxRate(), transparencyMetrics.taxAmount(),
                transparencyMetrics.sellerCommissionRate(), transparencyMetrics.sellerCommissionAmount(),
                transparencyMetrics.transparencyScore(), Instant.now().toString()
        );
    }
    
    private boolean shouldIncludeFees(Boolean includeFees) {
        return includeFees != null && includeFees;
    }

    private boolean shouldIncludeTaxes(Boolean includeTaxes) {
        return includeTaxes != null && includeTaxes;
    }

    private boolean shouldIncludeCommissions(Boolean includeCommissions) {
        return includeCommissions != null && includeCommissions;
    }
    
    private double calculateTransparencyScore(org.sky.service.stats.calculators.template.PaymentTransparencyRequest request) {
        var score = TRANSPARENCY_SCORE;
        
        // Ajustar score basado en qué información se incluye
        if (shouldIncludeFees(request.includeFees())) {
            score += 1.0; // Bonus por incluir fees
        }
        
        if (shouldIncludeTaxes(request.includeTaxes())) {
            score += 2.0; // Bonus por incluir taxes
        }
        
        if (shouldIncludeCommissions(request.includeCommissions())) {
            score += 2.0; // Bonus por incluir comisiones
        }
        
        return Math.min(100.0, score);
    }
    
    /**
     * Métricas específicas para transparency
     */
    private record TransparencySpecificMetrics(
        Double processingFees,
        Double platformFees,
        Double taxRate,
        Double taxAmount,
        Double sellerCommissionRate,
        Double sellerCommissionAmount,
        Double transparencyScore
    ) {}
}

