package io.github.joaodallagnol.backend.storage;

public enum StorageScope {
    PUBLIC("public"),
    PRIVATE("private");

    private final String value;

    StorageScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static StorageScope fromValue(String value) {
        for (StorageScope scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown storage scope.");
    }
}
