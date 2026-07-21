package io.github.joaodallagnol.backend.storage;

import io.github.joaodallagnol.backend.feature.FeatureFlagProperties;
import io.github.joaodallagnol.backend.session.SessionPhoto;
import io.github.joaodallagnol.backend.session.SessionPhotoRepository;
import io.github.joaodallagnol.backend.session.SessionVisibility;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class SessionPhotoStorageScopeWorker {

    private final FeatureFlagProperties featureFlags;
    private final SessionPhotoRepository photoRepository;
    private final R2ObjectStorage objectStorage;
    private final MediaCachePurger cachePurger;
    private final PhotoStorageDeletionRepository deletionRepository;

    public SessionPhotoStorageScopeWorker(FeatureFlagProperties featureFlags, SessionPhotoRepository photoRepository,
                                          R2ObjectStorage objectStorage, MediaCachePurger cachePurger,
                                          PhotoStorageDeletionRepository deletionRepository) {
        this.featureFlags = featureFlags;
        this.photoRepository = photoRepository;
        this.objectStorage = objectStorage;
        this.cachePurger = cachePurger;
        this.deletionRepository = deletionRepository;
    }

    @Scheduled(fixedDelayString = "${app.photo-processing.visibility-poll-delay-ms:5000}")
    @Transactional
    public void reconcileStorageScopes() {
        if (!featureFlags.isPhotoProcessing()) {
            return;
        }
        photoRepository.findPhotosAwaitingStorageScopeChange(PageRequest.of(0, 10))
                .forEach(photo -> moveOne(photo.getId()));
    }

    void moveOne(UUID photoId) {
        SessionPhoto photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || !"ready".equals(photo.getProcessingStatus())) {
            return;
        }
        StorageScope source = photo.getStorageScope();
        StorageScope target = photo.getSession().getVisibility() == SessionVisibility.EVERYONE
                ? StorageScope.PUBLIC : StorageScope.PRIVATE;
        if (source == target) {
            return;
        }
        objectStorage.copy(photo.getStorageKeyOriginal(), source, target);
        objectStorage.copy(photo.getStorageKeyThumbnail(), source, target);
        if (source == StorageScope.PUBLIC) {
            cachePurger.purge(photo.getStorageKeyOriginal(), photo.getStorageKeyThumbnail());
        }
        photo.moveTo(target);
        photoRepository.save(photo);
        deletionRepository.saveAll(List.of(
                new PhotoStorageDeletion(source, photo.getStorageKeyOriginal()),
                new PhotoStorageDeletion(source, photo.getStorageKeyThumbnail())
        ));
    }
}
