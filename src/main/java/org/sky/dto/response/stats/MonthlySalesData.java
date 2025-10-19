package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record MonthlySalesData(
    String month,
    Double sales,
    Long transactions
) {}
