package org.sky.service.stats.calculators.factory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.sky.service.stats.calculators.strategy.CalculationStrategy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory Pattern: Crea y gestiona las estrategias de cálculo
 * Clean Code: Principio de responsabilidad única - solo se encarga de la creación de estrategias
 */
@ApplicationScoped
public class StatsCalculatorFactory {
    
    @Inject
    Instance<CalculationStrategy<?>> strategies;
    
    /**
     * Obtiene todas las estrategias disponibles
     */
    public List<CalculationStrategy<?>> getAllStrategies() {
        return strategies.stream().collect(Collectors.toList());
    }
    
    /**
     * Obtiene una estrategia específica por nombre
     */
    public CalculationStrategy<?> getStrategy(String strategyName) {
        return strategies.stream()
            .filter(strategy -> strategy.getStrategyName().equals(strategyName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Strategy not found: " + strategyName));
    }
    
    /**
     * Verifica si existe una estrategia con el nombre dado
     */
    public boolean hasStrategy(String strategyName) {
        return strategies.stream()
            .anyMatch(strategy -> strategy.getStrategyName().equals(strategyName));
    }
}
