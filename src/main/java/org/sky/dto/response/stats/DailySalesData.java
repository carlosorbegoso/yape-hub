package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record DailySalesData(
    String date,
    String dayName,
    Double sales,
    Long transactions
) {}
