package io.github.joaodallagnol.backend.export;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/export")
public class UserDataExportController {
    private final UserDataExportService service;

    public UserDataExportController(UserDataExportService service) { this.service = service; }

    @GetMapping("/json")
    public UserDataExportResponse json() { return service.json(); }

    @GetMapping(value = "/sessions.csv", produces = "text/csv")
    public ResponseEntity<String> sessionsCsv() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sessions.csv")
                .body(service.sessionsCsv());
    }
}
