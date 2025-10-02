package org.sky.service.stats.calculators.template;

import java.time.LocalDate;

/**
 * Request object para cálculos de estadísticas de seller
 */
public record SellerStatsRequest(
    Long sellerId,
    LocalDate startDate,
    LocalDate endDate
) {
    public SellerStatsRequest {
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
