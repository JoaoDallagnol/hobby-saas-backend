package io.github.joaodallagnol.backend.user;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CurrentUserProfileResponse(
        UUID id,
        String email,
        String name,
        boolean emailVerified,
        String bio,
        OffsetDateTime createdAt
) {
    public static CurrentUserProfileResponse from(ProductUser productUser) {
        return new CurrentUserProfileResponse(
                productUser.getId(),
                productUser.getEmail(),
                productUser.getName(),
                productUser.isEmailVerified(),
                productUser.getBio(),
                productUser.getCreatedAt()
        );
    }
}
