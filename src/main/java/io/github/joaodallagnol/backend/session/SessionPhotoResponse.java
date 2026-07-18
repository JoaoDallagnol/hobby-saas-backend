package io.github.joaodallagnol.backend.session;

public record SessionPhotoResponse(
        String storageKeyOriginal,
        String storageKeyThumbnail
) {
    public static SessionPhotoResponse from(SessionPhoto photo) {
        return new SessionPhotoResponse(photo.getStorageKeyOriginal(), photo.getStorageKeyThumbnail());
    }
}
