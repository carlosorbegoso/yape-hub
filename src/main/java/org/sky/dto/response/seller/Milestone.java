package org.sky.dto.response.seller;

public record Milestone(
    String type,
    String date,
    Boolean achieved,
    Double value
) {}