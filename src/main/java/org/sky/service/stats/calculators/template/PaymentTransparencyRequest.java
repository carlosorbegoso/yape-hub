package org.sky.service.stats.calculators.template;

import java.time.LocalDate;

/**
 * Request object para cálculos de transparencia de pagos
 */
public record PaymentTransparencyRequest(
    Long adminId,
    LocalDate startDate,
    LocalDate endDate,
    Boolean includeFees,
    Boolean includeTaxes,
    Boolean includeCommissions
) {
    public PaymentTransparencyRequest {
        if (adminId == null) {
            throw new IllegalArgumentException("Admin ID es requerido");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas son requeridas");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }
}
