package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Milestone(
    String type,
    String date,
    Boolean achieved,
    Double value
) {}
