package org.sky.dto.request.admin;

import java.time.LocalDate;

public record AdminAnalyticsRequest(
    Long adminId,
    LocalDate startDate,
    LocalDate endDate,
    String include,
    String period,
    String metric,
    String granularity,
    Double confidence,
    Integer days
) {}