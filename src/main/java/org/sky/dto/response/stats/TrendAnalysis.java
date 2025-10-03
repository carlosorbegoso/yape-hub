package org.sky.dto.response.stats;

public record TrendAnalysis(
    String trend,
    Double slope,
    Double r2,
    Double forecastAccuracy
) {
    public static TrendAnalysis empty() {
        return new TrendAnalysis("", 0.0, 0.0, 0.0);
    }
}