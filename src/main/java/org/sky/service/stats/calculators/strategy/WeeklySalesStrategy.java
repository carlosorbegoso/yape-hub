package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.dto.response.stats.WeeklySalesData;
import org.sky.model.PaymentNotificationEntity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strategy Pattern: Implementación específica para datos de ventas semanales
 * Clean Code: Responsabilidad única - solo calcula ventas semanales
 */
@ApplicationScoped
public class WeeklySalesStrategy implements CalculationStrategy<List<WeeklySalesData>> {
    
    private static final Logger log = Logger.getLogger(WeeklySalesStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;
    
    @Override
    public Uni<List<WeeklySalesData>> calculate(List<PaymentNotificationEntity> payments, 
                                              LocalDate startDate, 
                                              LocalDate endDate, 
                                              Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 WeeklySalesStrategy: Calculando ventas semanales para " + payments.size() + " pagos");
            
            Map<LocalDate, Double> weeklySales = new ConcurrentHashMap<>();
            Map<LocalDate, Long> weeklyTransactions = new ConcurrentHashMap<>();
            
            processPayments(payments, startDate, endDate, weeklySales, weeklyTransactions);
            List<WeeklySalesData> result = generateWeeklySalesData(startDate, endDate, weeklySales, weeklyTransactions);
            
            log.debug("✅ WeeklySalesStrategy: " + result.size() + " semanas calculadas");
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
        return "WeeklySalesStrategy";
    }
    
    private void processPayments(List<PaymentNotificationEntity> payments,
                               LocalDate startDate,
                               LocalDate endDate,
                               Map<LocalDate, Double> weeklySales,
                               Map<LocalDate, Long> weeklyTransactions) {
        
        var stream = payments.size() > PARALLEL_THRESHOLD ? 
            payments.parallelStream() : payments.stream();
            
        stream.filter(payment -> isInDateRange(payment.createdAt.toLocalDate(), startDate, endDate))
              .forEach(payment -> {
                  LocalDate date = payment.createdAt.toLocalDate();
                  LocalDate weekStart = date.with(DayOfWeek.MONDAY);
                  double amount = getValidAmount(payment.amount);
                  weeklySales.merge(weekStart, amount, Double::sum);
                  weeklyTransactions.merge(weekStart, 1L, Long::sum);
              });
    }
    
    private boolean isInDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return date.isAfter(startDate.minusWeeks(1)) && date.isBefore(endDate.plusWeeks(1));
    }
    
    private double getValidAmount(Double amount) {
        return (amount != null && amount > 0) ? amount : 0.0;
    }
    
    private List<WeeklySalesData> generateWeeklySalesData(LocalDate startDate,
                                                        LocalDate endDate,
                                                        Map<LocalDate, Double> weeklySales,
                                                        Map<LocalDate, Long> weeklyTransactions) {
        
        List<LocalDate> weekRange = new ArrayList<>();
        LocalDate currentWeek = startDate.with(DayOfWeek.MONDAY);
        while (!currentWeek.isAfter(endDate)) {
            weekRange.add(currentWeek);
            currentWeek = currentWeek.plusWeeks(1);
        }
        
        return weekRange.parallelStream()
            .map(weekStart -> {
                double sales = weeklySales.getOrDefault(weekStart, 0.0);
                long transactions = weeklyTransactions.getOrDefault(weekStart, 0L);
                return new WeeklySalesData(weekStart.toString(), sales, transactions);
            })
            .toList();
    }
}
