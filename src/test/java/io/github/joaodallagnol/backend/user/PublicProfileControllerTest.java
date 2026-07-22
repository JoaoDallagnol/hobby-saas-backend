package io.github.joaodallagnol.backend.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.joaodallagnol.backend.session.PublicSessionPageResponse;
import io.github.joaodallagnol.backend.session.PublicSessionResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PublicProfileControllerTest {

    private StubPublicProfileService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = new StubPublicProfileService();
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicProfileController(service)).build();
    }

    @Test
    void shouldExposePublicProfileWithoutPrivateIdentityFields() throws Exception {
        service.profile = new PublicProfileResponse("runner.one", "Runner", "Bio", List.of());

        mockMvc.perform(get("/api/users/runner.one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("runner.one"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void shouldExposeOnlySafePublicSessionShape() throws Exception {
        UUID sessionId = UUID.randomUUID();
        PublicSessionResponse session = new PublicSessionResponse(
                sessionId, UUID.randomUUID(), "Running", "Morning", OffsetDateTime.parse("2026-07-20T10:00:00Z"),
                30, "Notes", 4, "Public Park", List.of(), Map.of("distance_km", 5)
        );
        service.sessions = new PublicSessionPageResponse(List.of(session), 0, 20, 1, 1, false);

        mockMvc.perform(get("/api/users/runner.one/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].locationLabel").value("Public Park"))
                .andExpect(jsonPath("$.items[0].projectId").doesNotExist())
                .andExpect(jsonPath("$.items[0].equipmentIds").doesNotExist())
                .andExpect(jsonPath("$.items[0].location.placeId").doesNotExist());
    }

    private static final class StubPublicProfileService extends PublicProfileService {
        private PublicProfileResponse profile;
        private PublicSessionPageResponse sessions;

        private StubPublicProfileService() {
            super(null, null, null, null);
        }

        @Override
        public PublicProfileResponse getProfile(String username) {
            return profile;
        }

        @Override
        public PublicSessionPageResponse listSessions(String username, UUID hobbyId, int page, int size) {
            return sessions;
        }
    }
}
