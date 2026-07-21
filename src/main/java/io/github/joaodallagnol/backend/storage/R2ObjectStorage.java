package io.github.joaodallagnol.backend.storage;

import java.nio.file.Path;

public interface R2ObjectStorage {

    void download(String storageKey, Path destination);

    void uploadWebp(String storageKey, Path source);

    void delete(String storageKey);

    default void download(String storageKey, Path destination, StorageScope scope) {
        download(storageKey, destination);
    }

    default void uploadWebp(String storageKey, Path source, StorageScope scope) {
        uploadWebp(storageKey, source);
    }

    default void delete(String storageKey, StorageScope scope) {
        delete(storageKey);
    }

    default void copy(String storageKey, StorageScope source, StorageScope target) {
        throw new UnsupportedOperationException("Storage copy is not implemented.");
    }
}
