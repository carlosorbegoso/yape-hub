package org.sky.dto.response.seller;

import java.util.List;

public record SellerAchievements(
    Long streakDays,
    Long bestStreak,
    Long totalStreaks,
    List<Milestone> milestones,
    List<Badge> badges
) {}