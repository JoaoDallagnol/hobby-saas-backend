package io.github.joaodallagnol.backend.storage;

import io.github.joaodallagnol.backend.feature.FeatureFlagProperties;
import io.github.joaodallagnol.backend.session.SessionPhoto;
import io.github.joaodallagnol.backend.session.SessionPhotoRepository;
import io.github.joaodallagnol.backend.session.SessionVisibility;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionPhotoProcessingWorker {

    private static final int MAX_ATTEMPTS = 3;

    private final FeatureFlagProperties featureFlags;
    private final SessionPhotoRepository photoRepository;
    private final R2ObjectStorage objectStorage;
    private final CwebpImageProcessor imageProcessor;

    public SessionPhotoProcessingWorker(
            FeatureFlagProperties featureFlags,
            SessionPhotoRepository photoRepository,
            R2ObjectStorage objectStorage,
            CwebpImageProcessor imageProcessor
    ) {
        this.featureFlags = featureFlags;
        this.photoRepository = photoRepository;
        this.objectStorage = objectStorage;
        this.imageProcessor = imageProcessor;
    }

    @Scheduled(fixedDelayString = "${app.photo-processing.poll-delay-ms:10000}")
    public void processPendingPhotos() {
        if (!featureFlags.isPhotoProcessing()) {
            return;
        }
        List<SessionPhoto> pending = photoRepository
                .findTop10ByProcessingStatusAndProcessingAttemptsLessThanOrderByIdAsc("pending", MAX_ATTEMPTS);
        pending.forEach(photo -> processOne(photo.getId()));
    }

    public void processOne(java.util.UUID photoId) {
        SessionPhoto photo = photoRepository.findById(photoId).orElse(null);
        if (photo == null || !"pending".equals(photo.getProcessingStatus())) {
            return;
        }

        Path directory = null;
        try {
            directory = Files.createTempDirectory("hobby-photo-");
            Path input = directory.resolve("input");
            Path processed = directory.resolve("original.webp");
            Path thumbnail = directory.resolve("thumbnail.webp");
            objectStorage.download(photo.getStorageKeyOriginal(), input, StorageScope.PRIVATE);
            imageProcessor.createWebp(input, processed, 2048, 82);
            imageProcessor.createWebp(input, thumbnail, 480, 75);

            String prefix = "processed/session-photos/" + photo.getId() + "/";
            String processedKey = prefix + "original.webp";
            String thumbnailKey = prefix + "thumbnail.webp";
            StorageScope targetScope = photo.getSession() != null
                    && photo.getSession().getVisibility() == SessionVisibility.EVERYONE
                    ? StorageScope.PUBLIC : StorageScope.PRIVATE;
            objectStorage.uploadWebp(processedKey, processed, targetScope);
            objectStorage.uploadWebp(thumbnailKey, thumbnail, targetScope);
            String temporaryKey = photo.getStorageKeyOriginal();
            photo.markReady(processedKey, thumbnailKey, targetScope);
            photoRepository.save(photo);
            try {
                objectStorage.delete(temporaryKey, StorageScope.PRIVATE);
            } catch (RuntimeException ignored) {
                // A stale temporary object is preferable to losing the processed DB references.
            }
        } catch (Exception ex) {
            photo.registerProcessingFailure(ex.getClass().getSimpleName());
            photoRepository.save(photo);
        } finally {
            cleanup(directory);
        }
    }

    private void cleanup(Path directory) {
        if (directory == null) {
            return;
        }
        try {
            Files.deleteIfExists(directory.resolve("input"));
            Files.deleteIfExists(directory.resolve("original.webp"));
            Files.deleteIfExists(directory.resolve("thumbnail.webp"));
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // The OS temporary directory cleanup is a fallback; no user data is logged here.
        }
    }
}
