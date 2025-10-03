package org.sky.dto.request.stats;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Request object para cálculos de estadísticas de seller
 */
public record SellerStatsRequest(
    @NotNull(message = "Seller ID es requerido")
    Long sellerId,
    
    @NotNull(message = "La fecha de inicio es requerida")
    LocalDate startDate,
    
    @NotNull(message = "La fecha de fin es requerida")
    LocalDate endDate
) {
}
