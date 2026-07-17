package io.github.joaodallagnol.backend.auth;

public record FirebaseVerifiedToken(
        String userId,
        String email,
        String name,
        boolean emailVerified
) {
}
