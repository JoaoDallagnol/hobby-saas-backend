package io.github.joaodallagnol.backend.gamification;

import java.util.List;

public record GamificationDashboardResponse(
        List<HobbyProgressResponse> hobbies,
        List<BadgeResponse> badges,
        PersonalRecordsResponse records,
        MonthlyChallengeResponse monthlyChallenge
) {
}
