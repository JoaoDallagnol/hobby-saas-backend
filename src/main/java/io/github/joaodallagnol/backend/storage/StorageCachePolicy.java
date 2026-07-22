package io.github.joaodallagnol.backend.storage;

public final class StorageCachePolicy {

    public static final String PUBLIC_IMMUTABLE = "public, max-age=31536000, immutable";
    public static final String PRIVATE_NO_STORE = "private, no-store";

    private StorageCachePolicy() {
    }

    public static String forScope(StorageScope scope) {
        return scope == StorageScope.PUBLIC ? PUBLIC_IMMUTABLE : PRIVATE_NO_STORE;
    }
}
