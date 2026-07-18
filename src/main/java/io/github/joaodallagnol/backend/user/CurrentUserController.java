package io.github.joaodallagnol.backend.user;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

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

    @PatchMapping
    public CurrentUserProfileResponse updateCurrentUserProfile(@Valid @RequestBody CurrentUserProfileUpdateRequest request) {
        return currentUserProfileService.updateCurrentUserProfile(request);
    }

    @GetMapping("/hobbies")
    public List<UserHobbyResponse> getCurrentUserHobbies() {
        return currentUserProfileService.getCurrentUserHobbies();
    }

    @PostMapping("/hobbies")
    @ResponseStatus(HttpStatus.CREATED)
    public UserHobbyResponse addCurrentUserHobby(@Valid @RequestBody AddUserHobbyRequest request) {
        return currentUserProfileService.addCurrentUserHobby(request);
    }

    @PatchMapping("/hobbies/{hobbyId}")
    public UserHobbyResponse updateCurrentUserHobby(
            @PathVariable UUID hobbyId,
            @Valid @RequestBody UpdateUserHobbyRequest request
    ) {
        return currentUserProfileService.updateCurrentUserHobby(hobbyId, request);
    }

    @DeleteMapping("/hobbies/{hobbyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCurrentUserHobby(@PathVariable UUID hobbyId) {
        currentUserProfileService.removeCurrentUserHobby(hobbyId);
    }
}
