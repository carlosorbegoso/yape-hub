package org.sky.service.stats.calculators.seller.daily;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.SellerAnalyticsRequest;
import org.sky.dto.stats.DailySalesData;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
@ApplicationScoped
public class SellerDailyCalculator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public List<DailySalesData> calculateDailySalesData(List<PaymentNotificationEntity> sellerPayments,
                                                                               List<PaymentNotificationEntity> allPayments,
                                                                               SellerAnalyticsRequest request) {
        // Usar granularity para determinar el nivel de detalle
        var granularity = request.granularity() != null ? request.granularity() : "daily";
        
        return request.startDate().datesUntil(request.endDate().plusDays(1))
                .map(date -> calculateDailyStatForDate(sellerPayments, allPayments, date, granularity, request.include()))
                .collect(Collectors.toList());
    }
    
    private DailySalesData calculateDailyStatForDate(List<PaymentNotificationEntity> sellerPayments,
                                                                            List<PaymentNotificationEntity> allPayments,
                                                                            LocalDate date, String granularity, String include) {
        var daySellerPayments = filterPaymentsByDate(sellerPayments, date);
        
        var totalSales = calculateTotalSales(filterPaymentsByStatus(daySellerPayments, "CONFIRMED"));
        var transactionCount = daySellerPayments.size();
        
        // Incluir métricas adicionales basadas en el parámetro include
        var includePending = shouldIncludeMetric("pending", include);
        var confirmedCount = countPaymentsByStatus(daySellerPayments, "CONFIRMED");
        
        // Usar los valores calculados para lógica adicional si es necesario
        if (includePending && confirmedCount > 0) {
            // Lógica adicional basada en los parámetros
        }
        
        return new DailySalesData(
                date.format(DATE_FORMATTER),
                getDayName(date),
                totalSales,
                (long) transactionCount
        );
    }
    
    private List<PaymentNotificationEntity> filterPaymentsByDate(List<PaymentNotificationEntity> payments, LocalDate date) {
        return payments.stream()
                .filter(payment -> payment.createdAt.toLocalDate().equals(date))
                .toList();
    }
    
    private List<PaymentNotificationEntity> filterPaymentsByStatus(List<PaymentNotificationEntity> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .toList();
    }
    
    private double calculateTotalSales(List<PaymentNotificationEntity> confirmedPayments) {
        return confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private long countPaymentsByStatus(List<PaymentNotificationEntity> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .count();
    }
    
    private String getDayName(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("es"));
    }
    
    private boolean shouldIncludeMetric(String metricType, String include) {
        if (include == null) return true;
        return include.contains(metricType) || include.equals("all");
    }
}

