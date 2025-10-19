package org.sky.dto.response.common;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PeriodInfo(
    String startDate,
    String endDate,
    int totalDays
) {}
