package org.sky.dto.response.seller;

import org.sky.dto.response.stats.PredictedSale;
import org.sky.dto.response.stats.TrendAnalysis;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerForecasting(
    List<PredictedSale> predictedSales,
    TrendAnalysis trendAnalysis,
    List<String> recommendations
) {
    public static SellerForecasting empty() {
        return new SellerForecasting(List.of(), TrendAnalysis.empty(), List.of());
    }
}
