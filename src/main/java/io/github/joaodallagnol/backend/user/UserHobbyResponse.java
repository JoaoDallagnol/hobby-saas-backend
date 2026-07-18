package io.github.joaodallagnol.backend.user;

import java.util.UUID;

public record UserHobbyResponse(
        UUID hobbyId,
        String hobbyName,
        String categoryName,
        String icon,
        String experienceLevel
) {
    public static UserHobbyResponse from(UserHobby userHobby) {
        return new UserHobbyResponse(
                userHobby.getHobby().getId(),
                userHobby.getHobby().getName(),
                userHobby.getHobby().getCategory().getName(),
                userHobby.getHobby().getIcon(),
                userHobby.getExperienceLevel()
        );
    }
}
