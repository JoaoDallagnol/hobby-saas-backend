package io.github.joaodallagnol.backend.auth;

import org.springframework.util.StringUtils;

public class LocalDevelopmentTokenVerifier implements FirebaseTokenVerifier {

    private final String expectedToken;
    private final FirebaseVerifiedToken verifiedToken;

    public LocalDevelopmentTokenVerifier(String expectedToken, FirebaseVerifiedToken verifiedToken) {
        this.expectedToken = expectedToken;
        this.verifiedToken = verifiedToken;
    }

    @Override
    public FirebaseVerifiedToken verify(String idToken) {
        if (!StringUtils.hasText(idToken) || !expectedToken.equals(idToken.trim())) {
            throw new IllegalArgumentException("Token is invalid.");
        }
        return verifiedToken;
    }
}
