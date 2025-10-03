package org.sky.dto.response.stats;

public record SalesDistribution(
    Double weekday,
    Double weekend,
    Double morning,
    Double afternoon,
    Double evening
) {}