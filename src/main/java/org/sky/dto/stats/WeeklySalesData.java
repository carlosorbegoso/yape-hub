package org.sky.dto.stats;

public record WeeklySalesData(
    String week,
    Double sales,
    Long transactions
) {}