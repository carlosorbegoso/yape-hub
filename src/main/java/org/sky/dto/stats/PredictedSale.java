package org.sky.dto.stats;

public record PredictedSale(
    String date,
    Double predicted,
    Double confidence
) {}