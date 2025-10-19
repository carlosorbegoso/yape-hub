package org.sky.dto.response.seller;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Badge(
    String name,
    String icon,
    String description,
    Boolean earned,
    String date
) {}
