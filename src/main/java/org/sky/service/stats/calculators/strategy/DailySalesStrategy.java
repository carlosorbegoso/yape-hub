package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.dto.response.stats.DailySalesData;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Strategy Pattern: Implementación específica para datos de ventas diarias
 * Clean Code: Responsabilidad única - solo calcula ventas diarias
 */
@ApplicationScoped
public class DailySalesStrategy implements CalculationStrategy<List<DailySalesData>> {
    
    private static final Logger log = Logger.getLogger(DailySalesStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;
    
    @Override
    public Uni<List<DailySalesData>> calculate(List<PaymentNotificationEntity> payments, 
                                             LocalDate startDate, 
                                             LocalDate endDate, 
                                             Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 DailySalesStrategy: Calculando ventas diarias para " + payments.size() + " pagos");
            
            Map<LocalDate, Double> dailySales = new ConcurrentHashMap<>();
            Map<LocalDate, Long> dailyTransactions = new ConcurrentHashMap<>();
            
            processPayments(payments, startDate, endDate, dailySales, dailyTransactions);
            List<DailySalesData> result = generateDailySalesData(startDate, endDate, dailySales, dailyTransactions);
            
            log.debug("✅ DailySalesStrategy: " + result.size() + " días calculados");
            return result;
        });
    }
    
    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments, 
                           LocalDate startDate, 
                           LocalDate endDate, 
                           Long adminId) {
        return payments != null && !payments.isEmpty() && 
               startDate != null && endDate != null;
    }
    
    @Override
    public String getStrategyName() {
        return "DailySalesStrategy";
    }
    
    // Métodos privados para mejorar legibilidad
    private void processPayments(List<PaymentNotificationEntity> payments, 
                               LocalDate startDate, 
                               LocalDate endDate,
                               Map<LocalDate, Double> dailySales, 
                               Map<LocalDate, Long> dailyTransactions) {
        
        var stream = payments.size() > PARALLEL_THRESHOLD ? 
            payments.parallelStream() : payments.stream();
            
        stream.filter(payment -> isInDateRange(payment.createdAt.toLocalDate(), startDate, endDate))
              .forEach(payment -> {
                  LocalDate date = payment.createdAt.toLocalDate();
                  double amount = getValidAmount(payment.amount);
                  dailySales.merge(date, amount, Double::sum);
                  dailyTransactions.merge(date, 1L, Long::sum);
              });
    }
    
    private boolean isInDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return date.isAfter(startDate.minusDays(1)) && date.isBefore(endDate.plusDays(1));
    }
    
    private double getValidAmount(Double amount) {
        return (amount != null && amount > 0) ? amount : 0.0;
    }
    
    private List<DailySalesData> generateDailySalesData(LocalDate startDate, 
                                                       LocalDate endDate,
                                                       Map<LocalDate, Double> dailySales, 
                                                       Map<LocalDate, Long> dailyTransactions) {
        
        List<LocalDate> dateRange = startDate.datesUntil(endDate.plusDays(1))
            .collect(Collectors.toList());
        
        return dateRange.parallelStream()
            .map(currentDate -> createDailySalesData(currentDate, dailySales, dailyTransactions))
            .collect(Collectors.toList());
    }
    
    private DailySalesData createDailySalesData(LocalDate currentDate, 
                                              Map<LocalDate, Double> dailySales, 
                                              Map<LocalDate, Long> dailyTransactions) {
        double sales = dailySales.getOrDefault(currentDate, 0.0);
        long transactions = dailyTransactions.getOrDefault(currentDate, 0L);
        
        return new DailySalesData(
            currentDate.toString(),
            currentDate.getDayOfWeek().toString(),
            sales,
            transactions
        );
    }
}
