package io.github.joaodallagnol.backend.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FeatureFlagServiceTest {

    @Test
    void shouldExposeConfiguredFlags() {
        FeatureFlagProperties properties = new FeatureFlagProperties();
        properties.setPhotoUploads(false);
        properties.setSessionLocation(true);
        properties.setPhotoProcessing(false);

        FeatureFlagsResponse response = new FeatureFlagService(properties).currentFlags();

        assertThat(response.photoUploads()).isFalse();
        assertThat(response.sessionLocation()).isTrue();
        assertThat(response.photoProcessing()).isFalse();
    }

    @Test
    void shouldFailSafelyWhenFeatureIsDisabled() {
        FeatureFlagProperties properties = new FeatureFlagProperties();
        properties.setPhotoUploads(false);

        assertThatThrownBy(() -> new FeatureFlagService(properties).requirePhotoUploads())
                .isInstanceOf(FeatureDisabledException.class)
                .hasMessageContaining("photoUploads");
    }
}
