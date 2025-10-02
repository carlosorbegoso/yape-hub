package org.sky.service.stats.calculators.strategy;

/**
 * Strategy interface para cálculos de estadísticas comunes
 * Permite intercambiar algoritmos de cálculo sin modificar el contexto
 */
public interface StatsCalculationStrategy<T, R> {
    
    /**
     * Ejecuta el cálculo específico sobre los datos de entrada
     * @param input Datos de entrada (pagos, fechas, etc.)
     * @return Resultado del cálculo
     */
    R calculate(T input);
    
    /**
     * Obtiene el tipo de cálculo que realiza esta estrategia
     * @return Tipo de cálculo
     */
    String getCalculationType();
    
    /**
     * Valida si la estrategia puede procesar el tipo de entrada dado
     * @param input Datos de entrada a validar
     * @return true si puede procesar, false en caso contrario
     */
    boolean canHandle(T input);
}
