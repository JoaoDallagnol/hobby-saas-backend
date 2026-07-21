package io.github.joaodallagnol.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joaodallagnol.backend.feature.FeatureFlagProperties;
import io.github.joaodallagnol.backend.session.SessionPhoto;
import io.github.joaodallagnol.backend.session.SessionPhotoRepository;
import io.github.joaodallagnol.backend.session.SessionRecord;
import io.github.joaodallagnol.backend.session.SessionVisibility;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionPhotoStorageScopeWorkerTest {

    @Test
    void shouldCopyPrivateVariantsToPublicAndQueuePrivateCleanup() {
        SessionRecord session = session(SessionVisibility.EVERYONE);
        SessionPhoto photo = new SessionPhoto(session, "uploads/temp.jpg");
        photo.markReady("processed/original.webp", "processed/thumbnail.webp", StorageScope.PRIVATE);
        AtomicInteger copies = new AtomicInteger();
        R2ObjectStorage storage = storage(copies);
        AtomicReference<List<PhotoStorageDeletion>> queued = new AtomicReference<>();
        PhotoStorageDeletionRepository deletions = deletionRepository(queued);

        worker(photo, storage, keys -> { }, deletions).moveOne(photo.getId());

        assertThat(photo.getStorageScope()).isEqualTo(StorageScope.PUBLIC);
        assertThat(copies).hasValue(2);
        assertThat(queued.get()).hasSize(2).allMatch(deletion -> deletion.getStorageScope() == StorageScope.PRIVATE);
    }

    @Test
    void shouldPurgeCacheWhenMovingPublicVariantsToPrivate() {
        SessionRecord session = session(SessionVisibility.ONLY_ME);
        SessionPhoto photo = new SessionPhoto(session, "uploads/temp.jpg");
        photo.markReady("processed/original.webp", "processed/thumbnail.webp", StorageScope.PUBLIC);
        AtomicInteger purges = new AtomicInteger();

        worker(photo, storage(new AtomicInteger()), keys -> purges.incrementAndGet(),
                deletionRepository(new AtomicReference<>())).moveOne(photo.getId());

        assertThat(photo.getStorageScope()).isEqualTo(StorageScope.PRIVATE);
        assertThat(purges).hasValue(1);
    }

    private SessionPhotoStorageScopeWorker worker(SessionPhoto photo, R2ObjectStorage storage,
                                                  MediaCachePurger purger,
                                                  PhotoStorageDeletionRepository deletions) {
        SessionPhotoRepository repository = (SessionPhotoRepository) Proxy.newProxyInstance(
                SessionPhotoRepository.class.getClassLoader(), new Class<?>[]{SessionPhotoRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(photo);
                    case "save" -> args[0];
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "SessionPhotoRepositoryStub";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new SessionPhotoStorageScopeWorker(new FeatureFlagProperties(), repository, storage, purger, deletions);
    }

    private R2ObjectStorage storage(AtomicInteger copies) {
        return new R2ObjectStorage() {
            public void download(String key, Path destination) { throw new UnsupportedOperationException(); }
            public void uploadWebp(String key, Path source) { throw new UnsupportedOperationException(); }
            public void delete(String key) { throw new UnsupportedOperationException(); }
            public void copy(String key, StorageScope source, StorageScope target) { copies.incrementAndGet(); }
        };
    }

    @SuppressWarnings("unchecked")
    private PhotoStorageDeletionRepository deletionRepository(
            AtomicReference<List<PhotoStorageDeletion>> queued
    ) {
        return (PhotoStorageDeletionRepository) Proxy.newProxyInstance(
                PhotoStorageDeletionRepository.class.getClassLoader(),
                new Class<?>[]{PhotoStorageDeletionRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "saveAll" -> {
                        List<PhotoStorageDeletion> values = (List<PhotoStorageDeletion>) args[0];
                        queued.set(values);
                        yield values;
                    }
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "PhotoStorageDeletionRepositoryStub";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private SessionRecord session(SessionVisibility visibility) {
        return new SessionRecord("user", null, "Session", OffsetDateTime.now(), 30, null, 4,
                null, null, Map.of(), visibility);
    }
}
