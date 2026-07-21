package io.github.joaodallagnol.backend.session;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SessionVisibility {
    EVERYONE("everyone"),
    ONLY_ME("only_me");

    private final String value;

    SessionVisibility(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SessionVisibility fromValue(String value) {
        for (SessionVisibility visibility : values()) {
            if (visibility.value.equals(value)) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Visibility must be 'everyone' or 'only_me'.");
    }
}
