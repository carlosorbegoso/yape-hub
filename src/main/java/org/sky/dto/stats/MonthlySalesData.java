package org.sky.dto.stats;

public record MonthlySalesData(
    String month,
    Double sales,
    Long transactions
) {}