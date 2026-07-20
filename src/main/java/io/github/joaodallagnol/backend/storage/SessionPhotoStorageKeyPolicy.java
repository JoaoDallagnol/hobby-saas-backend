package io.github.joaodallagnol.backend.storage;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class SessionPhotoStorageKeyPolicy {

    private SessionPhotoStorageKeyPolicy() {
    }

    public static String uploadPrefix(String userId) {
        String encodedUserId = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(userId.getBytes(StandardCharsets.UTF_8));
        return "uploads/users/" + encodedUserId + "/session-temp/";
    }

    public static void requireOwnedUploadKey(String userId, String storageKey) {
        if (storageKey == null || !storageKey.startsWith(uploadPrefix(userId))) {
            throw new IllegalArgumentException("Photo storage key does not belong to the authenticated user.");
        }
    }
}
