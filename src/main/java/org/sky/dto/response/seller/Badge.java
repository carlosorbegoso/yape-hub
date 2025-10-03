package org.sky.dto.response.seller;

public record Badge(
    String name,
    String icon,
    String description,
    Boolean earned,
    String date
) {}