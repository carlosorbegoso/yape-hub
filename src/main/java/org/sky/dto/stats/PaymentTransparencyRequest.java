package org.sky.dto.stats;

import java.time.LocalDate;

public record PaymentTransparencyRequest(
    Long adminId,
    LocalDate startDate,
    LocalDate endDate,
    Boolean includeFees,
    Boolean includeTaxes,
    Boolean includeCommissions
) {
    // Los campos del record son automáticamente públicos y finales
}

