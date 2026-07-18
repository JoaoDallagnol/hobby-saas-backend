package io.github.joaodallagnol.backend.storage;

public interface SessionPhotoUploadSigner {

    GeneratedUploadUrl signUpload(String storageKey, String contentType);
}
