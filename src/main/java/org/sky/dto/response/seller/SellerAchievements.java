package org.sky.dto.response.seller;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SellerAchievements(
    Long streakDays,
    Long bestStreak,
    Long totalStreaks,
    List<Milestone> milestones,
    List<Badge> badges
) {
    public static SellerAchievements empty() {
        return new SellerAchievements(0L, 0L, 0L, List.of(), List.of());
    }
}
