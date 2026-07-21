package io.github.joaodallagnol.backend.gamification;

import java.util.Arrays;

public enum GoalCadence {
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    CUSTOM("custom");

    private final String value;

    GoalCadence(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static GoalCadence from(String value) {
        return Arrays.stream(values()).filter(item -> item.value.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("cadence must be 'weekly', 'monthly' or 'custom'."));
    }
}
