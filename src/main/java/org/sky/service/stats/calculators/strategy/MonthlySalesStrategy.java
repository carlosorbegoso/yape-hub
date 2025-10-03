package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.dto.response.stats.MonthlySalesData;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strategy Pattern: Implementación específica para datos de ventas mensuales
 * Clean Code: Responsabilidad única - solo calcula ventas mensuales
 */
@ApplicationScoped
public class MonthlySalesStrategy implements CalculationStrategy<List<MonthlySalesData>> {
    
    private static final Logger log = Logger.getLogger(MonthlySalesStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;
    
    @Override
    public Uni<List<MonthlySalesData>> calculate(List<PaymentNotificationEntity> payments, 
                                               LocalDate startDate, 
                                               LocalDate endDate, 
                                               Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 MonthlySalesStrategy: Calculando ventas mensuales para " + payments.size() + " pagos");
            
            Map<String, Double> monthlySales = new ConcurrentHashMap<>();
            Map<String, Long> monthlyTransactions = new ConcurrentHashMap<>();
            
            processPayments(payments, startDate, endDate, monthlySales, monthlyTransactions);
            List<MonthlySalesData> result = generateMonthlySalesData(startDate, endDate, monthlySales, monthlyTransactions);
            
            log.debug("✅ MonthlySalesStrategy: " + result.size() + " meses calculados");
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
        return "MonthlySalesStrategy";
    }
    
    private void processPayments(List<PaymentNotificationEntity> payments,
                               LocalDate startDate,
                               LocalDate endDate,
                               Map<String, Double> monthlySales,
                               Map<String, Long> monthlyTransactions) {
        
        var stream = payments.size() > PARALLEL_THRESHOLD ? 
            payments.parallelStream() : payments.stream();
            
        stream.filter(payment -> isInDateRange(payment.createdAt.toLocalDate(), startDate, endDate))
              .forEach(payment -> {
                  LocalDate date = payment.createdAt.toLocalDate();
                  String monthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                  double amount = getValidAmount(payment.amount);
                  monthlySales.merge(monthKey, amount, Double::sum);
                  monthlyTransactions.merge(monthKey, 1L, Long::sum);
              });
    }
    
    private boolean isInDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return date.isAfter(startDate.minusMonths(1)) && date.isBefore(endDate.plusMonths(1));
    }
    
    private double getValidAmount(Double amount) {
        return (amount != null && amount > 0) ? amount : 0.0;
    }
    
    private List<MonthlySalesData> generateMonthlySalesData(LocalDate startDate,
                                                          LocalDate endDate,
                                                          Map<String, Double> monthlySales,
                                                          Map<String, Long> monthlyTransactions) {
        
        List<String> monthRange = new ArrayList<>();
        LocalDate currentMonth = startDate.withDayOfMonth(1);
        while (!currentMonth.isAfter(endDate)) {
            String monthKey = currentMonth.getYear() + "-" + String.format("%02d", currentMonth.getMonthValue());
            monthRange.add(monthKey);
            currentMonth = currentMonth.plusMonths(1);
        }
        
        return monthRange.parallelStream()
            .map(monthKey -> {
                double sales = monthlySales.getOrDefault(monthKey, 0.0);
                long transactions = monthlyTransactions.getOrDefault(monthKey, 0L);
                return new MonthlySalesData(monthKey, sales, transactions);
            })
            .toList();
    }
}
