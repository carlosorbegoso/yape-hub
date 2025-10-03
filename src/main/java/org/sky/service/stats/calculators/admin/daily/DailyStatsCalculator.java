package org.sky.service.stats.calculators.admin.daily;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.SalesStatsResponse;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
@ApplicationScoped
public class DailyStatsCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public List<SalesStatsResponse.DailyStats> calculateDailyStats(List<PaymentNotificationEntity> payments,
                                                                 LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> calculateDailyStatForDate(payments, date))
                .toList();
    }
    
    private SalesStatsResponse.DailyStats calculateDailyStatForDate(List<PaymentNotificationEntity> payments, LocalDate date) {
        var dayPayments = filterPaymentsByDate(payments, date);
        
        var daySales = calculateDaySales(dayPayments);
        var transactionCount = (long) dayPayments.size();
        var averageValue = calculateAverageValue(daySales, transactionCount);
        
        return new SalesStatsResponse.DailyStats(
                date.format(DATE_FORMATTER),
                daySales,
                transactionCount,
                averageValue
        );
    }
    
    private List<PaymentNotificationEntity> filterPaymentsByDate(List<PaymentNotificationEntity> payments, LocalDate date) {
        return payments.stream()
                .filter(payment -> payment.createdAt.toLocalDate().equals(date))
                .toList();
    }
    
    private double calculateDaySales(List<PaymentNotificationEntity> dayPayments) {
        return dayPayments.stream()
                .filter(payment -> CONFIRMED_STATUS.equals(payment.status))
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private double calculateAverageValue(double daySales, long transactionCount) {
        return transactionCount > 0 ? daySales / transactionCount : 0.0;
    }
}

