package io.github.joaodallagnol.backend.user;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/profile-customization")
public class ProfileCustomizationController {
    private final ProfileCustomizationService service;

    public ProfileCustomizationController(ProfileCustomizationService service) { this.service = service; }

    @GetMapping
    public ProfileCustomizationResponse current() { return service.current(); }

    @PatchMapping
    public ProfileCustomizationResponse update(@Valid @RequestBody ProfileCustomizationRequest request) {
        return service.update(request);
    }
}
