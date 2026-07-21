package io.github.joaodallagnol.backend.session;

import io.github.joaodallagnol.backend.storage.SessionPhotoReadSigner;
import io.github.joaodallagnol.backend.storage.StorageScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SessionPhotoMediaService {

    private final SessionPhotoReadSigner privateReadSigner;
    private final String publicBaseUrl;

    public SessionPhotoMediaService(
            SessionPhotoReadSigner privateReadSigner,
            @Value("${app.integrations.r2.public-base-url:}") String publicBaseUrl
    ) {
        this.privateReadSigner = privateReadSigner;
        this.publicBaseUrl = StringUtils.trimTrailingCharacter(publicBaseUrl, '/');
    }

    public SessionPhotoResponse toOwnerResponse(SessionPhoto photo) {
        return toResponse(photo, false);
    }

    public SessionPhotoResponse toPublicResponse(SessionPhoto photo) {
        return toResponse(photo, true);
    }

    private SessionPhotoResponse toResponse(SessionPhoto photo, boolean publicView) {
        if (!"ready".equals(photo.getProcessingStatus())) {
            return new SessionPhotoResponse(photo.getId(), null, null, photo.getProcessingStatus(), "processing");
        }
        StorageScope requiredScope = photo.getSession().getVisibility() == SessionVisibility.EVERYONE
                ? StorageScope.PUBLIC : StorageScope.PRIVATE;
        if (photo.getStorageScope() != requiredScope) {
            return new SessionPhotoResponse(photo.getId(), null, null, photo.getProcessingStatus(), "updating_visibility");
        }
        if (publicView && requiredScope != StorageScope.PUBLIC) {
            return new SessionPhotoResponse(photo.getId(), null, null, photo.getProcessingStatus(), "unavailable");
        }
        String originalUrl = requiredScope == StorageScope.PUBLIC
                ? publicUrl(photo.getStorageKeyOriginal()) : privateReadSigner.signPrivateRead(photo.getStorageKeyOriginal());
        String thumbnailUrl = requiredScope == StorageScope.PUBLIC
                ? publicUrl(photo.getStorageKeyThumbnail()) : privateReadSigner.signPrivateRead(photo.getStorageKeyThumbnail());
        return new SessionPhotoResponse(photo.getId(), originalUrl, thumbnailUrl, photo.getProcessingStatus(), "ready");
    }

    private String publicUrl(String storageKey) {
        return StringUtils.hasText(publicBaseUrl) ? publicBaseUrl + "/" + storageKey : null;
    }
}
