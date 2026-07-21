package io.github.joaodallagnol.backend.gamification;

public enum GoalStatus {
    ACTIVE("active"),
    COMPLETED("completed"),
    ARCHIVED("archived");

    private final String value;

    GoalStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
