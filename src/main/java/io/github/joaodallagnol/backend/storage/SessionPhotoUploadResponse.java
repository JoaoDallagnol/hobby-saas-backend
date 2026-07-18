package io.github.joaodallagnol.backend.storage;

import java.time.OffsetDateTime;
import java.util.Map;

public record SessionPhotoUploadResponse(
        String storageKey,
        String uploadUrl,
        String method,
        Map<String, String> requiredHeaders,
        OffsetDateTime expiresAt
) {
}
