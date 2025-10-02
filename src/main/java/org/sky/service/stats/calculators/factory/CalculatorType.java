package org.sky.service.stats.calculators.factory;

/**
 * Enum que define los tipos de calculadoras disponibles en el sistema.
 * Cada tipo corresponde a un tipo específico de análisis estadístico.
 */
public enum CalculatorType {
    /**
     * Calculadora para estadísticas de administrador
     */
    ADMIN_STATS("admin-stats"),
    
    /**
     * Calculadora para estadísticas de vendedor
     */
    SELLER_STATS("seller-stats"),
    
    /**
     * Calculadora para resumen rápido
     */
    QUICK_SUMMARY("quick-summary"),
    
    /**
     * Calculadora para análisis financiero
     */
    FINANCIAL_ANALYTICS("financial-analytics"),
    
    /**
     * Calculadora para transparencia de pagos
     */
    PAYMENT_TRANSPARENCY("payment-transparency"),
    
    /**
     * Calculadora para análisis financiero de vendedor
     */
    SELLER_FINANCIAL("seller-financial"),
    
    /**
     * Calculadora para análisis de administrador
     */
    ADMIN_ANALYTICS("admin-analytics"),
    
    /**
     * Calculadora para análisis de vendedor
     */
    SELLER_ANALYTICS("seller-analytics");
    
    private final String identifier;
    
    CalculatorType(String identifier) {
        this.identifier = identifier;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    /**
     * Obtiene el tipo de calculadora por su identificador
     */
    public static CalculatorType fromIdentifier(String identifier) {
        for (CalculatorType type : values()) {
            if (type.identifier.equals(identifier)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de calculadora no encontrado: " + identifier);
    }
}
