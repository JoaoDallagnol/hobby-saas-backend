package io.github.joaodallagnol.backend.feature;

import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

    private final FeatureFlagProperties properties;

    public FeatureFlagService(FeatureFlagProperties properties) {
        this.properties = properties;
    }

    public FeatureFlagsResponse currentFlags() {
        return new FeatureFlagsResponse(
                properties.isPhotoUploads(),
                properties.isSessionLocation(),
                properties.isPhotoProcessing()
        );
    }

    public void requirePhotoUploads() {
        require(properties.isPhotoUploads(), "photoUploads");
    }

    public void requireSessionLocation() {
        require(properties.isSessionLocation(), "sessionLocation");
    }

    private void require(boolean enabled, String feature) {
        if (!enabled) {
            throw new FeatureDisabledException(feature);
        }
    }
}
