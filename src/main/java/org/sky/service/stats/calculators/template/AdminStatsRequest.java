package org.sky.service.stats.calculators.template;

import java.time.LocalDate;

/**
 * Request object para cálculos de estadísticas de admin
 */
public record AdminStatsRequest(
    Long adminId,
    LocalDate startDate,
    LocalDate endDate
) {
    public AdminStatsRequest {
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
