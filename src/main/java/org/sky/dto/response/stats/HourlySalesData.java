package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record HourlySalesData(
    String hour,
    Double sales,
    Long transactions
) {}
