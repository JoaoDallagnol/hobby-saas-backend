package io.github.joaodallagnol.backend.auth;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthenticatedUserExtractor {

    public AuthenticatedUser extract(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof FirebaseAuthenticatedPrincipal principal)) {
            throw new IllegalArgumentException("Authenticated principal is not backed by a Firebase token.");
        }
        String subject = principal.id();
        String email = principal.email();
        String name = firstNonBlank(principal.name(), fallbackNameFromEmail(email));
        boolean emailVerified = principal.emailVerified();

        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("Firebase subject/uid is missing.");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Firebase email claim is missing.");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Firebase name/display name is missing.");
        }

        return new AuthenticatedUser(subject, email, name, emailVerified);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String fallbackNameFromEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return null;
        }
        return email.substring(0, email.indexOf('@'));
    }
}
