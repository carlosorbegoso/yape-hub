package org.sky.dto.stats;

import java.util.List;

public record SellerForecasting(
    List<PredictedSale> predictedSales,
    TrendAnalysis trendAnalysis,
    List<String> recommendations
) {}