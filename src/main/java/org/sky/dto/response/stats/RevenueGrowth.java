package org.sky.dto.response.stats;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record RevenueGrowth(
    Double daily,
    Double weekly,
    Double monthly,
    Double yearly
) {
    public static RevenueGrowth empty() {
        return new RevenueGrowth(0.0, 0.0, 0.0, 0.0);
    }
}
