package org.sky.dto.stats;

public record SalesDistribution(
    Double weekday,
    Double weekend,
    Double morning,
    Double afternoon,
    Double evening
) {}