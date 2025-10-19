package org.sky.dto.response.admin;

import java.time.LocalDate;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AdminStatsResponse(
    Long adminId,
    Double totalSales,
    Long totalTransactions,
    Double averageTransactionValue,
    LocalDate startDate,
    LocalDate endDate
) {}
