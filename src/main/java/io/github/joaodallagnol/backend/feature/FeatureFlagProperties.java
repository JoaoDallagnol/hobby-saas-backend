package io.github.joaodallagnol.backend.feature;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.features")
public class FeatureFlagProperties {

    private boolean photoUploads = true;
    private boolean sessionLocation = true;
    private boolean photoProcessing = false;

    public boolean isPhotoUploads() {
        return photoUploads;
    }

    public void setPhotoUploads(boolean photoUploads) {
        this.photoUploads = photoUploads;
    }

    public boolean isSessionLocation() {
        return sessionLocation;
    }

    public void setSessionLocation(boolean sessionLocation) {
        this.sessionLocation = sessionLocation;
    }

    public boolean isPhotoProcessing() {
        return photoProcessing;
    }

    public void setPhotoProcessing(boolean photoProcessing) {
        this.photoProcessing = photoProcessing;
    }
}
