package io.github.joaodallagnol.backend.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class CurrentUserController {

    private final CurrentUserProfileService currentUserProfileService;

    public CurrentUserController(CurrentUserProfileService currentUserProfileService) {
        this.currentUserProfileService = currentUserProfileService;
    }

    @GetMapping
    public CurrentUserProfileResponse getCurrentUserProfile() {
        return currentUserProfileService.getCurrentUserProfile();
    }
}
