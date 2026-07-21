package io.github.joaodallagnol.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record CurrentUserProfileUpdateRequest(
        @NotBlank
        @Size(max = 255)
        String name,
        @Size(max = 2000)
        String bio,
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[a-z0-9](?:[a-z0-9._-]{1,28}[a-z0-9])$",
                message = "username must use 3-30 lowercase letters, numbers, dots, underscores or hyphens")
        String username
) {
    public CurrentUserProfileUpdateRequest(String name, String bio) {
        this(name, bio, null);
    }
}
