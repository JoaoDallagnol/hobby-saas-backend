package io.github.joaodallagnol.backend.storage;

public interface SessionPhotoReadSigner {
    String signPrivateRead(String storageKey);
}
