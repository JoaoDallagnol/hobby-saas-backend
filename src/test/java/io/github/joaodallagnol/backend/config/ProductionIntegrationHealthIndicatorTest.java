package io.github.joaodallagnol.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joaodallagnol.backend.feature.FeatureFlagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

class ProductionIntegrationHealthIndicatorTest {

    @Test
    void shouldBeDownWhenEnabledIntegrationsHaveNoCredentials() {
        FeatureFlagProperties flags = new FeatureFlagProperties();
        flags.setPhotoUploads(true);
        flags.setSessionLocation(true);

        var indicator = new ProductionIntegrationHealthIndicator(
                flags, "", "", "", "", "", "", "", ""
        );

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails().get("missing").toString())
                .contains("firebase.project-id", "r2.bucket", "google.places-api-key");
    }

    @Test
    void shouldAllowDisabledOptionalIntegrations() {
        FeatureFlagProperties flags = new FeatureFlagProperties();
        flags.setPhotoUploads(false);
        flags.setSessionLocation(false);

        var indicator = new ProductionIntegrationHealthIndicator(
                flags, "firebase-prod", "service-account-base64", "", "", "", "", "", ""
        );

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }
}
