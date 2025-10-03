package org.sky.service.stats.calculators;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.model.PaymentNotificationEntity;
import org.sky.service.stats.calculators.strategy.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Clase base que implementa el patrón Template Method
 * Define el algoritmo común para calcular estadísticas usando estrategias
 */
@ApplicationScoped
public abstract class BaseStatsCalculator<T, R> {
    
    @Inject
    protected SalesCalculationStrategy salesStrategy;
    
    @Inject
    protected TransactionCountStrategy transactionCountStrategy;
    
    @Inject
    protected AverageTransactionValueStrategy averageTransactionValueStrategy;
    
    @Inject
    protected PaymentStatusCountStrategy paymentStatusCountStrategy;
    
    @Inject
    protected ClaimRateCalculationStrategy claimRateStrategy;
    
    @Inject
    protected TaxCalculationStrategy taxCalculationStrategy;
    
    @Inject
    protected CommissionCalculationStrategy commissionCalculationStrategy;
    
    @Inject
    protected FeeCalculationStrategy feeCalculationStrategy;
    
    /**
     * Template Method que define el algoritmo común para calcular estadísticas
     */
    public final R calculateStats(List<PaymentNotificationEntity> payments, T request) {
        // 1. Validar entrada
        validateInput(payments, request);
        
        // 2. Filtrar pagos si es necesario
        List<PaymentNotificationEntity> filteredPayments = filterPayments(payments, request);
        
        // 3. Calcular métricas básicas usando estrategias
        Double totalSales = salesStrategy.calculate(filteredPayments);
        Long totalTransactions = transactionCountStrategy.calculate(filteredPayments);
        Double averageTransactionValue = averageTransactionValueStrategy.calculate(filteredPayments);
        Double claimRate = claimRateStrategy.calculate(filteredPayments);
        
        // 4. Calcular métricas específicas
        Object specificMetrics = calculateSpecificMetrics(filteredPayments, request);
        
        // 5. Construir respuesta
        return buildResponse(totalSales, totalTransactions, averageTransactionValue, 
                           claimRate, specificMetrics, filteredPayments, request);
    }
    
    /**
     * Valida la entrada de datos (implementación específica por calculadora)
     */
    protected abstract void validateInput(List<PaymentNotificationEntity> payments, T request);
    
    /**
     * Filtra los pagos según criterios específicos (implementación específica por calculadora)
     */
    protected abstract List<PaymentNotificationEntity> filterPayments(List<PaymentNotificationEntity> payments, T request);
    
    /**
     * Calcula métricas específicas de cada tipo de calculadora
     */
    protected abstract Object calculateSpecificMetrics(List<PaymentNotificationEntity> payments, T request);
    
    /**
     * Construye la respuesta final (implementación específica por calculadora)
     */
    protected abstract R buildResponse(Double totalSales, Long totalTransactions,
                                       Double averageTransactionValue, Double claimRate,
                                       Object specificMetrics, List<PaymentNotificationEntity> payments, T request);
    
    /**
     * Método helper para contar pagos por estado
     */
    protected Long countPaymentsByStatus(List<PaymentNotificationEntity> payments, String status) {
        return paymentStatusCountStrategy.calculate(
            new PaymentStatusInput(payments, status)
        );
    }
    
    /**
     * Método helper para validar fechas
     */
    protected void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son requeridas");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }
    
    /**
     * Método helper para obtener el nombre del vendedor
     */
    protected String getSellerName(org.sky.model.SellerEntity seller) {
        return seller.sellerName != null ? seller.sellerName : "Sin nombre";
    }
    
    /**
     * Método helper para calcular impuestos
     */
    protected Double calculateTax(Double amount, Double taxRate) {
        return taxCalculationStrategy.calculate(new TaxInput(amount, taxRate));
    }
    
    /**
     * Método helper para calcular comisiones
     */
    protected Double calculateCommission(Double amount, Double commissionRate) {
        return commissionCalculationStrategy.calculate(new CommissionInput(amount, commissionRate));
    }
    
    /**
     * Método helper para calcular fees
     */
    protected Double calculateFee(Double amount, Double feeRate) {
        return feeCalculationStrategy.calculate(new FeeInput(amount, feeRate));
    }
}
