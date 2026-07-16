package io.github.joaodallagnol.backend.auth;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        String name,
        boolean emailVerified
) {
}
