package org.sky.dto.stats;

public record DailySalesData(
    String date,
    String dayName,
    Double sales,
    Long transactions
) {}