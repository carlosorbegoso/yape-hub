package org.sky.service.stats.calculators.template;

import java.time.LocalDate;

/**
 * Request object para cálculos financieros de seller
 */
public record SellerFinancialRequest(
    Long sellerId,
    LocalDate startDate,
    LocalDate endDate,
    String include,
    String currency,
    Double commissionRate
) {
    public SellerFinancialRequest {
        if (sellerId == null) {
            throw new IllegalArgumentException("Seller ID es requerido");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas son requeridas");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }
}
