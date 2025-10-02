package org.sky.service.stats.calculators.template;

import java.time.LocalDate;

/**
 * Request object para cálculos de analytics de seller
 */
public record SellerAnalyticsRequest(
    Long sellerId,
    LocalDate startDate,
    LocalDate endDate,
    String period,
    String granularity,
    String metric,
    Double confidence,
    Integer days,
    String include
) {
    public SellerAnalyticsRequest {
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
