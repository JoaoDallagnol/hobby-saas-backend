package io.github.joaodallagnol.backend.auth;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthenticatedUserExtractor {

    public AuthenticatedUser extract(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new IllegalArgumentException("Authenticated principal is not backed by a JWT token.");
        }

        return extract(jwtAuthenticationToken.getToken());
    }

    public AuthenticatedUser extract(Jwt jwt) {
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = firstNonBlank(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username"));
        boolean emailVerified = Boolean.TRUE.equals(jwt.getClaim("email_verified"));

        if (!StringUtils.hasText(subject)) {
            throw new IllegalArgumentException("JWT subject is missing.");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("JWT email claim is missing.");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("JWT name claim is missing.");
        }

        return new AuthenticatedUser(UUID.fromString(subject), email, name, emailVerified);
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
