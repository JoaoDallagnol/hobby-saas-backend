package io.github.joaodallagnol.backend.user;

import java.time.OffsetDateTime;

public record CurrentUserProfileResponse(
        String id,
        String email,
        String name,
        String username,
        boolean emailVerified,
        String bio,
        OffsetDateTime createdAt
) {
    public CurrentUserProfileResponse(String id, String email, String name, boolean emailVerified, String bio,
                                      OffsetDateTime createdAt) {
        this(id, email, name, null, emailVerified, bio, createdAt);
    }

    public static CurrentUserProfileResponse from(ProductUser productUser) {
        return new CurrentUserProfileResponse(
                productUser.getId(),
                productUser.getEmail(),
                productUser.getName(),
                productUser.getUsername(),
                productUser.isEmailVerified(),
                productUser.getBio(),
                productUser.getCreatedAt()
        );
    }
}
