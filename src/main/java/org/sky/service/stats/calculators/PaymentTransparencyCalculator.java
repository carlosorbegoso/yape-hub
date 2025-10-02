package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.PaymentTransparencyRequest;
import org.sky.dto.stats.PaymentTransparencyResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class PaymentTransparencyCalculator {

    private static final String CONFIRMED_STATUS = "CONFIRMED";
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
        return paymentNotificationRepository.findByAdminIdAndDateRange(
                request.adminId(), request.startDate().atStartOfDay(), request.endDate().atTime(23, 59, 59))
                .chain(payments -> sellerRepository.findByAdminId(request.adminId())
                        .map(sellers -> buildTransparencyReport(payments, sellers, request)));
    }

    private PaymentTransparencyResponse buildTransparencyReport(List<PaymentNotification> payments, 
                                                              List<Seller> sellers, 
                                                              PaymentTransparencyRequest request) {
        var confirmedPayments = filterConfirmedPayments(payments);
        var totalRevenue = calculateTotalRevenue(confirmedPayments);
        var period = createPeriod(request.startDate(), request.endDate());
        var totalTransactions = (long) payments.size();
        var confirmedTransactions = (long) confirmedPayments.size();

        // Calcular fees si están incluidos
        var processingFees = shouldIncludeFees(request.includeFees()) ? 
                calculateProcessingFees(totalRevenue) : null;
        var platformFees = shouldIncludeFees(request.includeFees()) ? 
                calculatePlatformFees(totalRevenue) : null;

        // Calcular taxes si están incluidos
        var taxRate = shouldIncludeTaxes(request.includeTaxes()) ? TAX_RATE : null;
        var taxAmount = shouldIncludeTaxes(request.includeTaxes()) ? 
                calculateTaxAmount(totalRevenue) : null;

        // Calcular comisiones si están incluidas
        var sellerCommissionRate = shouldIncludeCommissions(request.includeCommissions()) ? 
                SELLER_COMMISSION_RATE : null;
        var sellerCommissionAmount = shouldIncludeCommissions(request.includeCommissions()) ? 
                calculateSellerCommissionAmount(totalRevenue) : null;

        var transparencyScore = calculateTransparencyScore(request);
        var lastUpdated = Instant.now().toString();

        return new PaymentTransparencyResponse(
                period, totalRevenue, totalTransactions, confirmedTransactions,
                processingFees, platformFees, taxRate, taxAmount,
                sellerCommissionRate, sellerCommissionAmount,
                transparencyScore, lastUpdated
        );
    }

    private List<PaymentNotification> filterConfirmedPayments(List<PaymentNotification> payments) {
        return payments.stream()
                .filter(payment -> CONFIRMED_STATUS.equals(payment.status))
                .toList();
    }

    private double calculateTotalRevenue(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
    }

    private PaymentTransparencyResponse.Period createPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return new PaymentTransparencyResponse.Period(
                startDate.toString(),
                endDate.toString()
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

    private double calculateProcessingFees(double totalRevenue) {
        return totalRevenue * PROCESSING_FEE_RATE;
    }

    private double calculatePlatformFees(double totalRevenue) {
        return totalRevenue * PLATFORM_FEE_RATE;
    }

    private double calculateTaxAmount(double totalRevenue) {
        return totalRevenue * TAX_RATE;
    }

    private double calculateSellerCommissionAmount(double totalRevenue) {
        return totalRevenue * SELLER_COMMISSION_RATE;
    }

    private double calculateTransparencyScore(PaymentTransparencyRequest request) {
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
}

