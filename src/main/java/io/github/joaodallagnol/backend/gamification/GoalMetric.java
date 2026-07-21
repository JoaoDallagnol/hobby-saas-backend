package io.github.joaodallagnol.backend.gamification;

import java.util.Arrays;

public enum GoalMetric {
    SESSIONS("sessions"),
    MINUTES("minutes");

    private final String value;

    GoalMetric(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static GoalMetric from(String value) {
        return Arrays.stream(values()).filter(item -> item.value.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("metric must be 'sessions' or 'minutes'."));
    }
}
