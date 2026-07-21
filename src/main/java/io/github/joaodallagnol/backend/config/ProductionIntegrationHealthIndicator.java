package io.github.joaodallagnol.backend.config;

import io.github.joaodallagnol.backend.feature.FeatureFlagProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
public class ProductionIntegrationHealthIndicator implements HealthIndicator {

    private final FeatureFlagProperties featureFlags;
    private final String firebaseProjectId;
    private final String firebaseServiceAccountJsonBase64;
    private final String firebaseServiceAccountPath;
    private final String r2Endpoint;
    private final String r2PrivateBucket;
    private final String r2PublicBucket;
    private final String r2PublicBaseUrl;
    private final String cloudflareZoneId;
    private final String cloudflareApiToken;
    private final String r2AccessKey;
    private final String r2SecretKey;
    private final String googlePlacesApiKey;

    @Autowired
    public ProductionIntegrationHealthIndicator(
            FeatureFlagProperties featureFlags,
            @Value("${app.auth.firebase.project-id:}") String firebaseProjectId,
            @Value("${app.auth.firebase.service-account-json-base64:}") String firebaseServiceAccountJsonBase64,
            @Value("${app.auth.firebase.service-account-path:}") String firebaseServiceAccountPath,
            @Value("${app.integrations.r2.endpoint:}") String r2Endpoint,
            @Value("${app.integrations.r2.private-bucket:}") String r2PrivateBucket,
            @Value("${app.integrations.r2.public-bucket:}") String r2PublicBucket,
            @Value("${app.integrations.r2.public-base-url:}") String r2PublicBaseUrl,
            @Value("${app.integrations.r2.access-key:}") String r2AccessKey,
            @Value("${app.integrations.r2.secret-key:}") String r2SecretKey,
            @Value("${app.integrations.r2.cloudflare-zone-id:}") String cloudflareZoneId,
            @Value("${app.integrations.r2.cloudflare-api-token:}") String cloudflareApiToken,
            @Value("${app.integrations.google.places-api-key:}") String googlePlacesApiKey
    ) {
        this.featureFlags = featureFlags;
        this.firebaseProjectId = firebaseProjectId;
        this.firebaseServiceAccountJsonBase64 = firebaseServiceAccountJsonBase64;
        this.firebaseServiceAccountPath = firebaseServiceAccountPath;
        this.r2Endpoint = r2Endpoint;
        this.r2PrivateBucket = r2PrivateBucket;
        this.r2PublicBucket = r2PublicBucket;
        this.r2PublicBaseUrl = r2PublicBaseUrl;
        this.r2AccessKey = r2AccessKey;
        this.r2SecretKey = r2SecretKey;
        this.cloudflareZoneId = cloudflareZoneId;
        this.cloudflareApiToken = cloudflareApiToken;
        this.googlePlacesApiKey = googlePlacesApiKey;
    }

    ProductionIntegrationHealthIndicator(FeatureFlagProperties featureFlags, String firebaseProjectId,
                                         String firebaseServiceAccountJsonBase64, String firebaseServiceAccountPath,
                                         String r2Endpoint, String r2Bucket, String r2AccessKey, String r2SecretKey,
                                         String googlePlacesApiKey) {
        this(featureFlags, firebaseProjectId, firebaseServiceAccountJsonBase64, firebaseServiceAccountPath,
                r2Endpoint, r2Bucket, r2Bucket, "https://media.example.invalid", r2AccessKey, r2SecretKey,
                "zone", "token", googlePlacesApiKey);
    }

    @Override
    public Health health() {
        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(firebaseProjectId)) {
            missing.add("firebase.project-id");
        }
        if (!StringUtils.hasText(firebaseServiceAccountJsonBase64)
                && !StringUtils.hasText(firebaseServiceAccountPath)) {
            missing.add("firebase.service-account");
        }
        if (featureFlags.isPhotoUploads()) {
            requireText(missing, "r2.endpoint", r2Endpoint);
            requireText(missing, "r2.private-bucket", r2PrivateBucket);
            requireText(missing, "r2.public-bucket", r2PublicBucket);
            requireText(missing, "r2.public-base-url", r2PublicBaseUrl);
            requireText(missing, "r2.access-key", r2AccessKey);
            requireText(missing, "r2.secret-key", r2SecretKey);
            requireText(missing, "r2.cloudflare-zone-id", cloudflareZoneId);
            requireText(missing, "r2.cloudflare-api-token", cloudflareApiToken);
            if (!featureFlags.isPhotoProcessing()) {
                missing.add("photo-processing.disabled-while-photo-uploads-enabled");
            }
        }
        if (featureFlags.isSessionLocation()) {
            requireText(missing, "google.places-api-key", googlePlacesApiKey);
        }

        if (!missing.isEmpty()) {
            return Health.down()
                    .withDetail("reason", "Required production integration configuration is missing")
                    .withDetail("missing", missing)
                    .build();
        }
        return Health.up()
                .withDetail("photoUploadsEnabled", featureFlags.isPhotoUploads())
                .withDetail("sessionLocationEnabled", featureFlags.isSessionLocation())
                .build();
    }

    private void requireText(List<String> missing, String name, String value) {
        if (!StringUtils.hasText(value)) {
            missing.add(name);
        }
    }
}
