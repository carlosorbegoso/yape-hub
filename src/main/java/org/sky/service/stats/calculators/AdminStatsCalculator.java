package org.sky.service.stats.calculators;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.dto.stats.SalesStatsResponse;
import org.sky.model.PaymentNotification;
import org.sky.model.Seller;
import org.sky.repository.PaymentNotificationRepository;
import org.sky.repository.SellerRepository;
import org.sky.service.stats.calculators.admin.summary.SummaryStatsCalculator;
import org.sky.service.stats.calculators.admin.daily.DailyStatsCalculator;
import org.sky.service.stats.calculators.admin.seller.SellerStatsCalculator;
import org.sky.service.stats.calculators.admin.period.PeriodInfoCalculator;
import org.sky.service.stats.calculators.admin.validation.DateRangeValidator;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AdminStatsCalculator {
    
    private static final int MAX_SELLERS_LIMIT = 100;
    
    @Inject
    PaymentNotificationRepository paymentNotificationRepository;
    
    @Inject
    SellerRepository sellerRepository;
    
    @Inject
    SummaryStatsCalculator summaryStatsCalculator;
    
    @Inject
    DailyStatsCalculator dailyStatsCalculator;
    
    @Inject
    SellerStatsCalculator sellerStatsCalculator;
    
    @Inject
    PeriodInfoCalculator periodInfoCalculator;
    
    @Inject
    DateRangeValidator dateRangeValidator;
    
    @WithTransaction
    public Uni<SalesStatsResponse> calculateAdminStats(Long adminId, LocalDate startDate, LocalDate endDate) {
        return dateRangeValidator.validateDateRange(startDate, endDate)
                .chain(() -> paymentNotificationRepository.findPaymentsForStatsByAdminId(
                        adminId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59)))
                .chain(payments -> sellerRepository.findByAdminId(adminId)
                        .map(sellers -> {
                            var activeSellers = sellers.stream()
                                    .filter(seller -> Boolean.TRUE.equals(seller.isActive))
                                    .limit(MAX_SELLERS_LIMIT)
                                    .toList();
                            return buildSalesStatsResponse(payments, activeSellers, startDate, endDate);
                        }));
    }
    
    private SalesStatsResponse buildSalesStatsResponse(List<PaymentNotification> payments, List<Seller> sellers,
                                                      LocalDate startDate, LocalDate endDate) {
        var summary = summaryStatsCalculator.calculateSummaryStats(payments);
        var dailyStats = dailyStatsCalculator.calculateDailyStats(payments, startDate, endDate);
        var sellerStats = sellerStatsCalculator.calculateSellerStats(payments, sellers);
        var period = periodInfoCalculator.createPeriodInfo(startDate, endDate);
        
        return new SalesStatsResponse(period, summary, dailyStats, sellerStats);
    }
}
