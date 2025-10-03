package org.sky.dto.response.seller;

import org.sky.dto.response.stats.DailyStats;

import java.util.List;

public record SellerSpecificMetrics(
    List<DailyStats> dailyStats
) {}