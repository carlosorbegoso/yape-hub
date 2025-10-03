package org.sky.dto.response.admin;

import java.time.LocalDate;

public record AdminStatsResponse(
    Long adminId,
    Double totalSales,
    Long totalTransactions,
    Double averageTransactionValue,
    LocalDate startDate,
    LocalDate endDate
) {}
