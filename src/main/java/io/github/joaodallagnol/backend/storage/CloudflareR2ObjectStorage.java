package io.github.joaodallagnol.backend.storage;

import java.net.URI;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.MetadataDirective;

@Component
public class CloudflareR2ObjectStorage implements R2ObjectStorage {

    private final String endpoint;
    private final String privateBucket;
    private final String publicBucket;
    private final String accessKey;
    private final String secretKey;

    public CloudflareR2ObjectStorage(
            @Value("${app.integrations.r2.endpoint:}") String endpoint,
            @Value("${app.integrations.r2.private-bucket:}") String privateBucket,
            @Value("${app.integrations.r2.public-bucket:}") String publicBucket,
            @Value("${app.integrations.r2.access-key:}") String accessKey,
            @Value("${app.integrations.r2.secret-key:}") String secretKey
    ) {
        this.endpoint = endpoint;
        this.privateBucket = privateBucket;
        this.publicBucket = publicBucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public void download(String storageKey, Path destination) {
        download(storageKey, destination, StorageScope.PRIVATE);
    }

    @Override
    public void download(String storageKey, Path destination, StorageScope scope) {
        try (S3Client client = client()) {
            client.getObject(
                    GetObjectRequest.builder().bucket(bucket(scope)).key(storageKey).build(),
                    ResponseTransformer.toFile(destination)
            );
        }
    }

    @Override
    public void uploadWebp(String storageKey, Path source) {
        uploadWebp(storageKey, source, StorageScope.PRIVATE);
    }

    @Override
    public void uploadWebp(String storageKey, Path source, StorageScope scope) {
        try (S3Client client = client()) {
            client.putObject(
                    PutObjectRequest.builder().bucket(bucket(scope)).key(storageKey).contentType("image/webp").build(),
                    RequestBody.fromFile(source)
            );
        }
    }

    @Override
    public void delete(String storageKey) {
        delete(storageKey, StorageScope.PRIVATE);
    }

    @Override
    public void delete(String storageKey, StorageScope scope) {
        try (S3Client client = client()) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket(scope)).key(storageKey).build());
        }
    }

    @Override
    public void copy(String storageKey, StorageScope source, StorageScope target) {
        try (S3Client client = client()) {
            client.copyObject(CopyObjectRequest.builder()
                    .copySource(bucket(source) + "/" + storageKey)
                    .destinationBucket(bucket(target))
                    .destinationKey(storageKey)
                    .contentType("image/webp")
                    .metadataDirective(MetadataDirective.REPLACE)
                    .build());
        }
    }

    private S3Client client() {
        if (!StringUtils.hasText(endpoint)
                || !StringUtils.hasText(privateBucket)
                || !StringUtils.hasText(publicBucket)
                || !StringUtils.hasText(accessKey)
                || !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("Cloudflare R2 object storage is not configured.");
        }
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private String bucket(StorageScope scope) {
        return scope == StorageScope.PUBLIC ? publicBucket : privateBucket;
    }
}
