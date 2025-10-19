package org.sky.dto.request.payment;

import java.time.LocalDate;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaymentTransparencyRequest(
    Long adminId,
    LocalDate startDate,
    LocalDate endDate,
    Boolean includeFees,
    Boolean includeTaxes,
    Boolean includeCommissions
) {}
