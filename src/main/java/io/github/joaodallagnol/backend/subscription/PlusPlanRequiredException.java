package io.github.joaodallagnol.backend.subscription;

public class PlusPlanRequiredException extends RuntimeException {

    public PlusPlanRequiredException() {
        super("An active Plus plan is required for this feature.");
    }
}
