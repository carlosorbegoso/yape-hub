package org.sky.dto.stats;

import java.util.List;

public record SellerAchievements(
    Long streakDays,
    Long bestStreak,
    Long totalStreaks,
    List<Milestone> milestones,
    List<Badge> badges
) {}