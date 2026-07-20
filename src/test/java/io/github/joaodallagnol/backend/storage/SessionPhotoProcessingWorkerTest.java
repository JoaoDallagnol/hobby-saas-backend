package io.github.joaodallagnol.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joaodallagnol.backend.feature.FeatureFlagProperties;
import io.github.joaodallagnol.backend.session.SessionPhoto;
import io.github.joaodallagnol.backend.session.SessionPhotoRepository;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class SessionPhotoProcessingWorkerTest {

    @Test
    void shouldCreateSanitizedVariantsAndMarkPhotoReady() {
        SessionPhoto photo = new SessionPhoto(null, "uploads/users/abc/session-temp/photo.jpg");
        AtomicBoolean saved = new AtomicBoolean();
        AtomicBoolean temporaryDeleted = new AtomicBoolean();
        R2ObjectStorage storage = storageThatWritesInput(temporaryDeleted, false);

        worker(photo, saved, storage).processOne(photo.getId());

        assertThat(photo.getProcessingStatus()).isEqualTo("ready");
        assertThat(photo.getStorageKeyOriginal()).endsWith("/original.webp");
        assertThat(photo.getStorageKeyThumbnail()).endsWith("/thumbnail.webp");
        assertThat(saved).isTrue();
        assertThat(temporaryDeleted).isTrue();
    }

    @Test
    void shouldTrackFailureWithoutExposingProviderMessage() {
        SessionPhoto photo = new SessionPhoto(null, "uploads/users/abc/session-temp/missing.jpg");
        AtomicBoolean saved = new AtomicBoolean();

        worker(photo, saved, storageThatWritesInput(new AtomicBoolean(), true)).processOne(photo.getId());

        assertThat(photo.getProcessingStatus()).isEqualTo("pending");
        assertThat(photo.getProcessingAttempts()).isEqualTo(1);
        assertThat(saved).isTrue();
    }

    private SessionPhotoProcessingWorker worker(
            SessionPhoto photo,
            AtomicBoolean saved,
            R2ObjectStorage storage
    ) {
        FeatureFlagProperties flags = new FeatureFlagProperties();
        flags.setPhotoProcessing(true);
        SessionPhotoRepository repository = (SessionPhotoRepository) Proxy.newProxyInstance(
                SessionPhotoRepository.class.getClassLoader(),
                new Class<?>[]{SessionPhotoRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(photo);
                    case "save" -> {
                        saved.set(true);
                        yield args[0];
                    }
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "SessionPhotoRepositoryStub";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        CwebpImageProcessor processor = new CwebpImageProcessor("unused") {
            @Override
            public void createWebp(Path input, Path output, int width, int quality) {
                try {
                    Files.writeString(output, "webp");
                } catch (java.io.IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        };
        return new SessionPhotoProcessingWorker(flags, repository, storage, processor);
    }

    private R2ObjectStorage storageThatWritesInput(AtomicBoolean deleted, boolean failDownload) {
        return new R2ObjectStorage() {
            @Override
            public void download(String storageKey, Path destination) {
                if (failDownload) {
                    throw new IllegalStateException("secret provider detail");
                }
                try {
                    Files.writeString(destination, "image");
                } catch (java.io.IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }

            @Override
            public void uploadWebp(String storageKey, Path source) {
                assertThat(Files.exists(source)).isTrue();
            }

            @Override
            public void delete(String storageKey) {
                deleted.set(true);
            }
        };
    }
}
