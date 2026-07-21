package io.github.joaodallagnol.backend.user;

import java.util.List;

public record PublicProfileResponse(
        String username,
        String name,
        String bio,
        List<UserHobbyResponse> hobbies,
        ProfileCustomizationResponse customization
) {
    public PublicProfileResponse(String username, String name, String bio, List<UserHobbyResponse> hobbies) {
        this(username, name, bio, hobbies, null);
    }
}
