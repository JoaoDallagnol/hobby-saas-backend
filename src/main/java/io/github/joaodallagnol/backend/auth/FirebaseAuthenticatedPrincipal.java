package io.github.joaodallagnol.backend.auth;

public record FirebaseAuthenticatedPrincipal(
        String id,
        String email,
        String name,
        boolean emailVerified
) {
}
