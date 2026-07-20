package io.github.joaodallagnol.backend.session;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SessionPhotoRequest(
        UUID id,
        @Size(max = 500)
        String storageKey
) {
    public SessionPhotoRequest(String storageKey) {
        this(null, storageKey);
    }
}
