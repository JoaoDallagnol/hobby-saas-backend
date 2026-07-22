package io.github.joaodallagnol.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CloudflareR2SessionPhotoUploadSignerTest {

    @Test
    void signsPrivateUploadWithNoStoreHeader() {
        CloudflareR2SessionPhotoUploadSigner signer = new CloudflareR2SessionPhotoUploadSigner(
                "http://localhost:9090",
                "private-bucket",
                "test-access-key",
                "test-secret-key"
        );

        GeneratedUploadUrl result = signer.signUpload("uploads/user/photo.jpg", "image/jpeg", 1234L);

        assertThat(result.method()).isEqualTo("PUT");
        assertThat(result.requiredHeaders())
                .containsEntry("Content-Type", "image/jpeg")
                .containsEntry("Content-Length", "1234")
                .containsEntry("Cache-Control", StorageCachePolicy.PRIVATE_NO_STORE);
    }
}
