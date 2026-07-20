package io.github.joaodallagnol.backend.storage;

import java.nio.file.Path;

public interface R2ObjectStorage {

    void download(String storageKey, Path destination);

    void uploadWebp(String storageKey, Path source);

    void delete(String storageKey);
}
