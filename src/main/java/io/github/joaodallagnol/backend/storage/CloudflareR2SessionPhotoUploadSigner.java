package io.github.joaodallagnol.backend.storage;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class CloudflareR2SessionPhotoUploadSigner implements SessionPhotoUploadSigner {

    private static final Duration EXPIRATION = Duration.ofMinutes(15);

    private final String presignEndpoint;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;

    public CloudflareR2SessionPhotoUploadSigner(
            @Value("${app.integrations.r2.presign-endpoint:${app.integrations.r2.endpoint:}}") String presignEndpoint,
            @Value("${app.integrations.r2.private-bucket:}") String bucket,
            @Value("${app.integrations.r2.access-key:}") String accessKey,
            @Value("${app.integrations.r2.secret-key:}") String secretKey
    ) {
        this.presignEndpoint = presignEndpoint;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public GeneratedUploadUrl signUpload(String storageKey, String contentType, long contentLength) {
        validateConfiguration();

        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(presignEndpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                    PutObjectPresignRequest.builder()
                            .signatureDuration(EXPIRATION)
                            .putObjectRequest(putObjectRequest)
                            .build()
            );

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", contentType);
            headers.put("Content-Length", Long.toString(contentLength));

            return new GeneratedUploadUrl(
                    presignedRequest.url().toString(),
                    "PUT",
                    headers,
                    OffsetDateTime.ofInstant(presignedRequest.expiration(), ZoneOffset.UTC)
            );
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(presignEndpoint)
                || !StringUtils.hasText(bucket)
                || !StringUtils.hasText(accessKey)
                || !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("Object storage is not fully configured.");
        }
    }
}
