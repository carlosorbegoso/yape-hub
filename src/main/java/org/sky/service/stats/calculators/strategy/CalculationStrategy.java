package org.sky.service.stats.calculators.strategy;

import io.smallrye.mutiny.Uni;
import org.sky.model.PaymentNotificationEntity;

import java.time.LocalDate;
import java.util.List;

/**
 * Strategy Pattern: Define la interfaz común para diferentes tipos de cálculos
 * Clean Code: Principio de responsabilidad única - cada estrategia tiene una responsabilidad específica
 */
public interface CalculationStrategy<T> {
    
    /**
     * Ejecuta el cálculo específico de la estrategia
     * @param payments Lista de pagos para procesar
     * @param startDate Fecha de inicio (opcional)
     * @param endDate Fecha de fin (opcional)
     * @param adminId ID del administrador (opcional)
     * @return Resultado del cálculo envuelto en Uni para programación reactiva
     */
    Uni<T> calculate(List<PaymentNotificationEntity> payments, 
                    LocalDate startDate, 
                    LocalDate endDate, 
                    Long adminId);
    
    /**
     * Valida si la estrategia puede manejar los parámetros dados
     */
    boolean canHandle(List<PaymentNotificationEntity> payments, 
                     LocalDate startDate, 
                     LocalDate endDate, 
                     Long adminId);
    
    /**
     * Obtiene el nombre de la estrategia para logging y debugging
     */
    String getStrategyName();
}
