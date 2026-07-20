package io.github.joaodallagnol.backend.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse createSession(@Valid @RequestBody CreateSessionRequest request) {
        return sessionService.createSession(request);
    }

    @GetMapping
    public SessionPageResponse listSessions(
            @RequestParam(required = false) UUID hobbyId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return sessionService.listSessions(hobbyId, page, size);
    }

    @GetMapping("/{sessionId}")
    public SessionResponse getSession(@PathVariable UUID sessionId) {
        return sessionService.getSession(sessionId);
    }

    @PatchMapping("/{sessionId}")
    public SessionResponse updateSession(@PathVariable UUID sessionId, @Valid @RequestBody UpdateSessionRequest request) {
        return sessionService.updateSession(sessionId, request);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable UUID sessionId) {
        sessionService.deleteSession(sessionId);
    }
}
