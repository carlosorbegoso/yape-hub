package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record WeeklySalesData(
    String week,
    Double sales,
    Long transactions
) {}
