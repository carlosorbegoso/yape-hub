package org.sky.service.stats.calculators.factory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.sky.service.stats.calculators.*;
import org.sky.service.stats.calculators.AdminStatsCalculator;

/**
 * Factory para crear instancias de calculadoras de estadísticas.
 * Utiliza el patrón Factory para centralizar la creación de calculadoras
 * y proporcionar una interfaz unificada para obtenerlas.
 */
@ApplicationScoped
public class CalculatorFactory {
    
    @Inject
    AdminStatsCalculator adminStatsCalculator;
    
    @Inject
    SellerStatsCalculator sellerStatsCalculator;
    
    @Inject
    QuickSummaryCalculator quickSummaryCalculator;
    
    @Inject
    FinancialAnalyticsCalculator financialAnalyticsCalculator;
    
    @Inject
    PaymentTransparencyCalculator paymentTransparencyCalculator;
    
    @Inject
    SellerFinancialCalculator sellerFinancialCalculator;
    
    @Inject
    AdminAnalyticsCalculator adminAnalyticsCalculator;
    
    @Inject
    SellerAnalyticsCalculator sellerAnalyticsCalculator;
    
    /**
     * Crea una calculadora basada en el tipo especificado
     * 
     * @param type Tipo de calculadora a crear
     * @return Instancia de la calculadora correspondiente
     * @throws IllegalArgumentException Si el tipo no es válido
     */
    public Object createCalculator(CalculatorType type) {
        return switch (type) {
            case ADMIN_STATS -> adminStatsCalculator;
            case SELLER_STATS -> sellerStatsCalculator;
            case QUICK_SUMMARY -> quickSummaryCalculator;
            case FINANCIAL_ANALYTICS -> financialAnalyticsCalculator;
            case PAYMENT_TRANSPARENCY -> paymentTransparencyCalculator;
            case SELLER_FINANCIAL -> sellerFinancialCalculator;
            case ADMIN_ANALYTICS -> adminAnalyticsCalculator;
            case SELLER_ANALYTICS -> sellerAnalyticsCalculator;
        };
    }
    
    /**
     * Crea una calculadora basada en el identificador del tipo
     * 
     * @param identifier Identificador del tipo de calculadora
     * @return Instancia de la calculadora correspondiente
     * @throws IllegalArgumentException Si el identificador no es válido
     */
    public Object createCalculator(String identifier) {
        var type = CalculatorType.fromIdentifier(identifier);
        return createCalculator(type);
    }
    
    /**
     * Obtiene la calculadora de estadísticas de administrador
     */
    public AdminStatsCalculator getAdminStatsCalculator() {
        return adminStatsCalculator;
    }
    
    /**
     * Obtiene la calculadora de estadísticas de vendedor
     */
    public SellerStatsCalculator getSellerStatsCalculator() {
        return sellerStatsCalculator;
    }
    
    /**
     * Obtiene la calculadora de resumen rápido
     */
    public QuickSummaryCalculator getQuickSummaryCalculator() {
        return quickSummaryCalculator;
    }
    
    /**
     * Obtiene la calculadora de análisis financiero
     */
    public FinancialAnalyticsCalculator getFinancialAnalyticsCalculator() {
        return financialAnalyticsCalculator;
    }
    
    /**
     * Obtiene la calculadora de transparencia de pagos
     */
    public PaymentTransparencyCalculator getPaymentTransparencyCalculator() {
        return paymentTransparencyCalculator;
    }
    
    /**
     * Obtiene la calculadora de análisis financiero de vendedor
     */
    public SellerFinancialCalculator getSellerFinancialCalculator() {
        return sellerFinancialCalculator;
    }
    
    /**
     * Obtiene la calculadora de análisis de administrador
     */
    public AdminAnalyticsCalculator getAdminAnalyticsCalculator() {
        return adminAnalyticsCalculator;
    }
    
    /**
     * Obtiene la calculadora de análisis de vendedor
     */
    public SellerAnalyticsCalculator getSellerAnalyticsCalculator() {
        return sellerAnalyticsCalculator;
    }
    
    /**
     * Verifica si un tipo de calculadora es válido
     */
    public boolean isValidCalculatorType(String identifier) {
        try {
            CalculatorType.fromIdentifier(identifier);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Obtiene todos los tipos de calculadoras disponibles
     */
    public CalculatorType[] getAvailableCalculatorTypes() {
        return CalculatorType.values();
    }
}
