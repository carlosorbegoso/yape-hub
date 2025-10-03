package org.sky.dto.response.stats;

public record HourlySalesData(
    String hour,
    Double sales,
    Long transactions
) {}