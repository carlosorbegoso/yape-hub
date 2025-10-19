package org.sky.dto.response.common;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Period(
    String startDate,
    String endDate
) {}
