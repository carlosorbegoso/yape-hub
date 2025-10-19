package org.sky.dto.response.stats;

import org.sky.dto.response.common.PeriodInfo;
import org.sky.dto.response.seller.SellerStats;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SalesStatsResponse(
    PeriodInfo period,
    SummaryStats summary,
    List<DailyStats> dailyStats,
    List<SellerStats> sellerStats
) {}
