package org.sky.dto.request.stats;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
/**
 * Request object para cálculos financieros de seller
 */
public record SellerFinancialRequest(
    @NotNull(message = "Seller ID es requerido")
    Long sellerId,
    
    @NotNull(message = "La fecha de inicio es requerida")
    LocalDate startDate,
    
    @NotNull(message = "La fecha de fin es requerida")
    LocalDate endDate,
    
    String include,
    String currency,
    Double commissionRate
) {
}
