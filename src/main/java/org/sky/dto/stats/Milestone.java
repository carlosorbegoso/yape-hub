package org.sky.dto.stats;

public record Milestone(
    String type,
    String date,
    Boolean achieved,
    Double value
) {}