package io.github.joaodallagnol.backend.gamification;

public record PersonalRecordsResponse(
        int longestSessionMinutes,
        long mostSessionsInAWeek,
        long mostMinutesInAMonth,
        int bestStreakDays,
        HobbyRecordResponse topHobbyBySessions,
        HobbyRecordResponse topHobbyByMinutes
) {
}
