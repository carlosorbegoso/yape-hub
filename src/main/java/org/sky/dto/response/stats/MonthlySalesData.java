package org.sky.dto.response.stats;

public record MonthlySalesData(
    String month,
    Double sales,
    Long transactions
) {}