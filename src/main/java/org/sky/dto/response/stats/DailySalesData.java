package org.sky.dto.response.stats;

public record DailySalesData(
    String date,
    String dayName,
    Double sales,
    Long transactions
) {}