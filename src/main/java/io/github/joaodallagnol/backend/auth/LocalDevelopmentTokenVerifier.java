package io.github.joaodallagnol.backend.auth;

import java.util.Map;
import org.springframework.util.StringUtils;

public class LocalDevelopmentTokenVerifier implements FirebaseTokenVerifier {

    private final Map<String, FirebaseVerifiedToken> usersByToken;

    public LocalDevelopmentTokenVerifier(String expectedToken, FirebaseVerifiedToken verifiedToken) {
        this(Map.of(expectedToken, verifiedToken));
    }

    public LocalDevelopmentTokenVerifier(Map<String, FirebaseVerifiedToken> usersByToken) {
        this.usersByToken = Map.copyOf(usersByToken);
    }

    @Override
    public FirebaseVerifiedToken verify(String idToken) {
        FirebaseVerifiedToken verifiedToken = StringUtils.hasText(idToken)
                ? usersByToken.get(idToken.trim())
                : null;
        if (verifiedToken == null) {
            throw new IllegalArgumentException("Token is invalid.");
        }
        return verifiedToken;
    }
}
