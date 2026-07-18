package io.github.joaodallagnol.backend.storage;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/session-photos")
public class SessionPhotoUploadController {

    private final SessionPhotoUploadService sessionPhotoUploadService;

    public SessionPhotoUploadController(SessionPhotoUploadService sessionPhotoUploadService) {
        this.sessionPhotoUploadService = sessionPhotoUploadService;
    }

    @PostMapping("/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionPhotoUploadResponse createUploadUrl(@Valid @RequestBody CreateSessionPhotoUploadRequest request) {
        return sessionPhotoUploadService.createUpload(request);
    }
}
