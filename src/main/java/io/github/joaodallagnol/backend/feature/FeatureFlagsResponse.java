package io.github.joaodallagnol.backend.feature;

public record FeatureFlagsResponse(
        boolean photoUploads,
        boolean sessionLocation,
        boolean photoProcessing
) {
}
