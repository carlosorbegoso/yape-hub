package org.sky.dto.response.seller;

import java.util.List;

public record SellerPerformance(
    String bestDay,
    String worstDay,
    Double averageDailySales,
    Double consistencyScore,
    List<String> peakPerformanceHours,
    Double productivityScore,
    Double efficiencyRate,
    Double responseTime
) {}