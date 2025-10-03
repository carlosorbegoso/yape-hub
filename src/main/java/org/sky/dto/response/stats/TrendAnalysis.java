package org.sky.dto.response.stats;

public record TrendAnalysis(
    String trend,
    Double slope,
    Double r2,
    Double forecastAccuracy
) {}