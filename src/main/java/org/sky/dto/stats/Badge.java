package org.sky.dto.stats;

public record Badge(
    String name,
    String icon,
    String description,
    Boolean earned,
    String date
) {}