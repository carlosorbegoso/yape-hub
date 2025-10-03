package org.sky.dto.response.common;

public record PeriodInfo(
    String startDate,
    String endDate,
    int totalDays
) {}
