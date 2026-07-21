package io.github.joaodallagnol.backend.storage;

public interface MediaCachePurger {
    void purge(String... storageKeys);
}
