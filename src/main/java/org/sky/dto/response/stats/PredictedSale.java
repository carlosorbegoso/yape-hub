package org.sky.dto.response.stats;

public record PredictedSale(
    String date,
    Double predicted,
    Double confidence
) {
    public static PredictedSale empty() {
        return new PredictedSale("", 0.0, 0.0);
    }
}