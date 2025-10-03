package org.sky.dto.request.stats;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request object para cálculos de estadísticas de admin
 */
public record AdminStatsRequest(
    @NotNull(message = "Admin ID es requerido")
    Long adminId,
    
    @NotNull(message = "La fecha de inicio es requerida")
    LocalDate startDate,
    
    @NotNull(message = "La fecha de fin es requerida")
    LocalDate endDate
) {
}
