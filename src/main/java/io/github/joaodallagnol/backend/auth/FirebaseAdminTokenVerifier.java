package io.github.joaodallagnol.backend.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.util.StringUtils;

public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAdminTokenVerifier(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public FirebaseVerifiedToken verify(String idToken) {
        if (!StringUtils.hasText(idToken)) {
            throw new IllegalArgumentException("Bearer token is missing.");
        }

        try {
            FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(idToken);
            return new FirebaseVerifiedToken(
                    firebaseToken.getUid(),
                    firebaseToken.getEmail(),
                    firstNonBlank(firebaseToken.getName(), firebaseToken.getEmail()),
                    Boolean.TRUE.equals(firebaseToken.isEmailVerified())
            );
        } catch (FirebaseAuthException ex) {
            throw new IllegalArgumentException("Firebase token is invalid.", ex);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
