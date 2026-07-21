package io.github.joaodallagnol.backend.user;

import io.github.joaodallagnol.backend.session.PublicSessionPageResponse;
import io.github.joaodallagnol.backend.session.PublicSessionResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users/{username}")
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    public PublicProfileController(PublicProfileService publicProfileService) {
        this.publicProfileService = publicProfileService;
    }

    @GetMapping
    public PublicProfileResponse getProfile(@PathVariable @Size(min = 3, max = 30) @Pattern(regexp = "^[a-z0-9._-]+$") String username) {
        return publicProfileService.getProfile(username);
    }

    @GetMapping("/sessions")
    public PublicSessionPageResponse listSessions(
            @PathVariable @Size(min = 3, max = 30) @Pattern(regexp = "^[a-z0-9._-]+$") String username,
            @RequestParam(required = false) UUID hobbyId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return publicProfileService.listSessions(username, hobbyId, page, size);
    }

    @GetMapping("/sessions/{sessionId}")
    public PublicSessionResponse getSession(
            @PathVariable @Size(min = 3, max = 30) @Pattern(regexp = "^[a-z0-9._-]+$") String username,
            @PathVariable UUID sessionId
    ) {
        return publicProfileService.getSession(username, sessionId);
    }
}
