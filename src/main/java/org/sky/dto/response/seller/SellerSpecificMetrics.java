package org.sky.dto.response.seller;

import org.sky.dto.response.stats.DailyStats;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerSpecificMetrics(
    List<DailyStats> dailyStats
) {}
