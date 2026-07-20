package io.github.joaodallagnol.backend.feature;

public class FeatureDisabledException extends RuntimeException {

    public FeatureDisabledException(String feature) {
        super("Feature is disabled in this environment: " + feature);
    }
}
