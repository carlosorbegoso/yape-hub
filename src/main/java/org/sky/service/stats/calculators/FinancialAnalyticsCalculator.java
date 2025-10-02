package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.FinancialAnalyticsRequest;
import org.sky.dto.stats.FinancialAnalyticsResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.util.List;

@ApplicationScoped
public class FinancialAnalyticsCalculator {

    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final String DEFAULT_CURRENCY = "PEN";
    private static final double DEFAULT_TAX_RATE = 0.18;

    @Inject
    PaymentNotificationRepository paymentNotificationRepository;

    @Inject
    SellerRepository sellerRepository;

    @WithTransaction
    public Uni<FinancialAnalyticsResponse> calculateFinancialAnalytics(FinancialAnalyticsRequest request) {
        return paymentNotificationRepository.findByAdminIdAndDateRange(
                request.adminId(), request.startDate().atStartOfDay(), request.endDate().atTime(23, 59, 59))
                .chain(payments -> sellerRepository.findByAdminId(request.adminId())
                        .map(sellers -> buildFinancialAnalyticsResponse(payments, sellers, request)));
    }

    private FinancialAnalyticsResponse buildFinancialAnalyticsResponse(List<PaymentNotification> payments, 
                                                                      List<Seller> sellers, 
                                                                      FinancialAnalyticsRequest request) {
        var confirmedPayments = filterConfirmedPayments(payments);
        var totalRevenue = calculateTotalRevenue(confirmedPayments);
        var currency = getCurrency(request.currency());
        var taxRate = getTaxRate(request.taxRate());
        var taxAmount = calculateTaxAmount(totalRevenue, taxRate);
        var netRevenue = calculateNetRevenue(totalRevenue, taxAmount);
        var period = createPeriod(request.startDate(), request.endDate());
        var transactions = (long) payments.size();
        var confirmedTransactions = (long) confirmedPayments.size();
        var averageTransactionValue = calculateAverageTransactionValue(payments);

        return new FinancialAnalyticsResponse(
                totalRevenue, currency, taxRate, taxAmount, netRevenue,
                period, request.include(), transactions, confirmedTransactions, averageTransactionValue
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

    private String getCurrency(String currency) {
        return currency != null ? currency : DEFAULT_CURRENCY;
    }

    private double getTaxRate(Double taxRate) {
        return taxRate != null ? taxRate : DEFAULT_TAX_RATE;
    }

    private double calculateTaxAmount(double totalRevenue, double taxRate) {
        return totalRevenue * taxRate;
    }

    private double calculateNetRevenue(double totalRevenue, double taxAmount) {
        return totalRevenue - taxAmount;
    }

    private FinancialAnalyticsResponse.Period createPeriod(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return new FinancialAnalyticsResponse.Period(
                startDate.toString(),
                endDate.toString()
        );
    }

    private double calculateAverageTransactionValue(List<PaymentNotification> payments) {
        return payments.stream()
                .mapToDouble(payment -> payment.amount)
                .average()
                .orElse(0.0);
    }
}

