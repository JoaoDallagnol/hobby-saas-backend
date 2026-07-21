package io.github.joaodallagnol.backend.storage;

import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Component
public class S3SessionPhotoReadSigner implements SessionPhotoReadSigner {

    private static final Duration EXPIRATION = Duration.ofMinutes(15);

    private final String endpoint;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;

    public S3SessionPhotoReadSigner(
            @Value("${app.integrations.r2.presign-endpoint:${app.integrations.r2.endpoint:}}") String endpoint,
            @Value("${app.integrations.r2.private-bucket:}") String bucket,
            @Value("${app.integrations.r2.access-key:}") String accessKey,
            @Value("${app.integrations.r2.secret-key:}") String secretKey
    ) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public String signPrivateRead(String storageKey) {
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(bucket)
                || !StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            return null;
        }
        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(EXPIRATION)
                    .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(storageKey).build())
                    .build()).url().toString();
        }
    }
}
