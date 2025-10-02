package org.sky.service.stats.calculators.admin.overview;

import jakarta.enterprise.context.ApplicationScoped;
import org.sky.dto.stats.AnalyticsSummaryResponse;
import org.sky.dto.stats.OverviewMetrics;
import org.sky.model.PaymentNotification;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class AdminOverviewCalculator {
    
    private static final String CONFIRMED_STATUS = "CONFIRMED";
    
    public OverviewMetrics calculateOverviewMetrics(List<PaymentNotification> payments, 
                                                                           LocalDate startDate, 
                                                                           LocalDate endDate) {
        var confirmedPayments = filterPaymentsByStatus(payments, CONFIRMED_STATUS);
        
        var totalSales = calculateTotalSales(confirmedPayments);
        var totalTransactions = (long) payments.size();
        var averageTransactionValue = calculateAverageTransactionValue(totalSales, totalTransactions);
        
        // Calcular crecimiento usando period y days del request
        var salesGrowth = calculateSalesGrowth(totalSales, startDate, endDate);
        var transactionGrowth = calculateTransactionGrowth(totalTransactions, startDate, endDate);
        var averageGrowth = calculateAverageGrowth(averageTransactionValue, startDate, endDate);
        
        return new OverviewMetrics(
                totalSales, totalTransactions, averageTransactionValue,
                salesGrowth, transactionGrowth, averageGrowth
        );
    }
    
    private List<PaymentNotification> filterPaymentsByStatus(List<PaymentNotification> payments, String status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.status))
                .toList();
    }
    
    private double calculateTotalSales(List<PaymentNotification> confirmedPayments) {
        return confirmedPayments.stream()
                .mapToDouble(payment -> payment.amount)
                .sum();
    }
    
    private double calculateAverageTransactionValue(double totalSales, long totalTransactions) {
        return totalTransactions > 0 ? totalSales / totalTransactions : 0.0;
    }
    
    private double calculateSalesGrowth(double currentSales, LocalDate startDate, LocalDate endDate) {
        // Implementación simplificada - en producción se compararía con período anterior
        var daysDiff = startDate.until(endDate).getDays();
        return daysDiff > 7 ? currentSales * 0.1 : currentSales * 0.05;
    }
    
    private double calculateTransactionGrowth(long currentTransactions, LocalDate startDate, LocalDate endDate) {
        // Implementación simplificada - en producción se compararía con período anterior
        var daysDiff = startDate.until(endDate).getDays();
        return daysDiff > 7 ? currentTransactions * 0.08 : currentTransactions * 0.03;
    }
    
    private double calculateAverageGrowth(double currentAverage, LocalDate startDate, LocalDate endDate) {
        // Implementación simplificada - en producción se compararía con período anterior
        var daysDiff = startDate.until(endDate).getDays();
        return daysDiff > 7 ? currentAverage * 0.05 : currentAverage * 0.02;
    }
}
