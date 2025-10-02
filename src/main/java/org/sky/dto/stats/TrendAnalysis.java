package org.sky.dto.stats;

public record TrendAnalysis(
    String trend,
    Double slope,
    Double r2,
    Double forecastAccuracy
) {}