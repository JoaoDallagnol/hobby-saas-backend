package io.github.joaodallagnol.backend.user;

import java.util.UUID;

public record HobbyCatalogResponse(
        UUID id,
        String name,
        String categoryName,
        String icon
) {

    static HobbyCatalogResponse from(Hobby hobby) {
        return new HobbyCatalogResponse(
                hobby.getId(),
                hobby.getName(),
                hobby.getCategory().getName(),
                hobby.getIcon()
        );
    }
}
