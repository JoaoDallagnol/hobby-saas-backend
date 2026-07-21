package io.github.joaodallagnol.backend.session;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionRequestValidationTest {

    @Test
    void shouldRejectMoreThanOnePhoto() {
        var request = new CreateSessionRequest(
                UUID.randomUUID(), "Session", OffsetDateTime.now(), 30, null, 4, null, null, List.of(),
                List.of(new SessionPhotoRequest(null, "uploads/users/a/one.jpg"),
                        new SessionPhotoRequest(null, "uploads/users/a/two.jpg")),
                Map.of(), SessionVisibility.ONLY_ME
        );
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request))
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("photos"));
        }
    }
}
