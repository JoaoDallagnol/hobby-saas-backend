package io.github.joaodallagnol.backend.backlog;

import java.util.Arrays;

public enum BacklogStatus {
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    DONE("done");

    private final String value;

    BacklogStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static BacklogStatus from(String raw) {
        return Arrays.stream(values())
                .filter(status -> status.value.equals(raw))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid backlog status."));
    }
}
