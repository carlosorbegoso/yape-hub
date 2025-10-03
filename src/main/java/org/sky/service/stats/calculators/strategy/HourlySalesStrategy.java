package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.sky.dto.response.stats.HourlySalesData;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Strategy Pattern: Implementación específica para datos de ventas por hora
 * Clean Code: Responsabilidad única - solo calcula ventas por hora
 */
@ApplicationScoped
public class HourlySalesStrategy implements CalculationStrategy<List<HourlySalesData>> {
    
    private static final Logger log = Logger.getLogger(HourlySalesStrategy.class);
    private static final int PARALLEL_THRESHOLD = 1000;
    
    @Override
    public Uni<List<HourlySalesData>> calculate(List<PaymentNotificationEntity> payments, 
                                              LocalDate startDate, 
                                              LocalDate endDate, 
                                              Long adminId) {
        return Uni.createFrom().item(() -> {
            log.debug("🔄 HourlySalesStrategy: Calculando ventas por hora para " + payments.size() + " pagos");
            
            Map<Integer, Double> hourlySales = new ConcurrentHashMap<>();
            Map<Integer, Long> hourlyTransactions = new ConcurrentHashMap<>();
            
            processPayments(payments, hourlySales, hourlyTransactions);
            List<HourlySalesData> result = generateHourlySalesData(hourlySales, hourlyTransactions);
            
            log.debug("✅ HourlySalesStrategy: " + result.size() + " horas calculadas");
            return result;
        });
    }
    
    @Override
    public boolean canHandle(List<PaymentNotificationEntity> payments, 
                           LocalDate startDate, 
                           LocalDate endDate, 
                           Long adminId) {
        return payments != null && !payments.isEmpty();
    }
    
    @Override
    public String getStrategyName() {
        return "HourlySalesStrategy";
    }
    
    private void processPayments(List<PaymentNotificationEntity> payments,
                               Map<Integer, Double> hourlySales,
                               Map<Integer, Long> hourlyTransactions) {
        
        var stream = payments.size() > PARALLEL_THRESHOLD ? 
            payments.parallelStream() : payments.stream();
            
        stream.filter(this::isValidPayment)
              .forEach(payment -> {
                  int hour = payment.createdAt.getHour();
                  double amount = getValidAmount(payment.amount);
                  hourlySales.merge(hour, amount, Double::sum);
                  hourlyTransactions.merge(hour, 1L, Long::sum);
              });
    }
    
    private boolean isValidPayment(PaymentNotificationEntity payment) {
        return payment.amount != null && payment.amount > 0;
    }
    
    private double getValidAmount(Double amount) {
        return (amount != null && amount > 0) ? amount : 0.0;
    }
    
    private List<HourlySalesData> generateHourlySalesData(Map<Integer, Double> hourlySales,
                                                        Map<Integer, Long> hourlyTransactions) {
        
        return IntStream.range(0, 24)
            .parallel()
            .mapToObj(hour -> {
                double sales = hourlySales.getOrDefault(hour, 0.0);
                long transactions = hourlyTransactions.getOrDefault(hour, 0L);
                return new HourlySalesData(String.valueOf(hour), sales, transactions);
            })
            .toList();
    }
}
