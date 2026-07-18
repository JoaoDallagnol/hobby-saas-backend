package io.github.joaodallagnol.backend.storage;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SessionPhotoUploadService {

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif"
    );

    private final AuthenticatedUserExtractor authenticatedUserExtractor;
    private final SessionPhotoUploadSigner sessionPhotoUploadSigner;

    public SessionPhotoUploadService(
            AuthenticatedUserExtractor authenticatedUserExtractor,
            SessionPhotoUploadSigner sessionPhotoUploadSigner
    ) {
        this.authenticatedUserExtractor = authenticatedUserExtractor;
        this.sessionPhotoUploadSigner = sessionPhotoUploadSigner;
    }

    public SessionPhotoUploadResponse createUpload(CreateSessionPhotoUploadRequest request) {
        String normalizedContentType = request.contentType().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException("Unsupported image content type.");
        }
        if (request.sizeBytes() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Image exceeds the maximum allowed size of 10 MB.");
        }

        AuthenticatedUser user = authenticatedUserExtractor.extract(SecurityContextHolder.getContext().getAuthentication());
        String storageKey = buildStorageKey(user.id(), normalizedContentType);
        GeneratedUploadUrl uploadUrl = sessionPhotoUploadSigner.signUpload(storageKey, normalizedContentType);

        return new SessionPhotoUploadResponse(
                storageKey,
                uploadUrl.uploadUrl(),
                uploadUrl.method(),
                uploadUrl.requiredHeaders(),
                uploadUrl.expiresAt()
        );
    }

    private String buildStorageKey(String userId, String contentType) {
        String safeUserId = userId.replaceAll("[^a-zA-Z0-9_-]", "_");
        String datePath = DateTimeFormatter.ofPattern("yyyy/MM/dd").format(OffsetDateTime.now(ZoneOffset.UTC));
        String extension = extensionFor(contentType);
        return "uploads/%s/session-temp/%s/%s.%s".formatted(
                safeUserId,
                datePath,
                UUID.randomUUID(),
                extension
        );
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/heic" -> "heic";
            case "image/heif" -> "heif";
            default -> throw new IllegalArgumentException("Unsupported image content type.");
        };
    }
}
