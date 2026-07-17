package io.github.joaodallagnol.backend.auth;

public class MissingFirebaseConfigurationTokenVerifier implements FirebaseTokenVerifier {

    private final String message;

    public MissingFirebaseConfigurationTokenVerifier(String message) {
        this.message = message;
    }

    @Override
    public FirebaseVerifiedToken verify(String idToken) {
        throw new IllegalStateException(message);
    }
}
