package org.sky.dto.stats;

public record HourlySalesData(
    String hour,
    Double sales,
    Long transactions
) {}