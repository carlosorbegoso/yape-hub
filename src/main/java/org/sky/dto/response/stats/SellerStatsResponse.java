package org.sky.dto.response.stats;

import org.sky.dto.response.common.PeriodInfo;
import org.sky.dto.response.seller.SellerSummaryStats;

import java.util.List;

public record SellerStatsResponse(
    Long sellerId,
    String sellerName,
    PeriodInfo period,
    SellerSummaryStats summary,
    List<DailyStats> dailyStats
) {}
