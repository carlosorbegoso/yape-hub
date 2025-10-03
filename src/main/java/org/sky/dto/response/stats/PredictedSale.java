package org.sky.dto.response.stats;

public record PredictedSale(
    String date,
    Double predicted,
    Double confidence
) {}