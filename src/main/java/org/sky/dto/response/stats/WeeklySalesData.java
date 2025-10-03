package org.sky.dto.response.stats;

public record WeeklySalesData(
    String week,
    Double sales,
    Long transactions
) {}