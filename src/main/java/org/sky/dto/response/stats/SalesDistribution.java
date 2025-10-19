package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SalesDistribution(
    Double weekday,
    Double weekend,
    Double morning,
    Double afternoon,
    Double evening
) {
    public static SalesDistribution empty() {
        return new SalesDistribution(0.0, 0.0, 0.0, 0.0, 0.0);
    }
}
