package io.github.joaodallagnol.backend.integration;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {PostgresIntegrationTestSupport.AuthTestConfig.class})
class SessionPlacePersistenceIntegrationTest extends PostgresIntegrationTestSupport {

    private static final UUID RUNNING_HOBBY_ID = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM session_equipment");
        jdbcTemplate.update("DELETE FROM session_photos");
        jdbcTemplate.update("DELETE FROM sessions");
        jdbcTemplate.update("DELETE FROM equipment");
        jdbcTemplate.update("DELETE FROM backlog_items");
        jdbcTemplate.update("DELETE FROM user_hobbies");
        jdbcTemplate.update("DELETE FROM hobby_suggestions");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");

        insertUserWithRunningHobby("test-user", "user@example.com", "Example User");
    }

    @Test
    void resolvesAndPersistsPlaceFromPlaceIdOnSessionCreate() throws Exception {
        String sessionPayload = """
                {
                  "hobbyId": "%s",
                  "title": "Park Run",
                  "startedAt": "%s",
                  "durationMinutes": 55,
                  "notes": "Route with elevation.",
                  "satisfaction": 4,
                  "location": {
                    "placeId": "place-abc",
                    "label": "Park Run Start"
                  },
                  "attributes": {
                    "distance_km": 10.1,
                    "avg_pace_min_km": 5.45,
                    "surface": "trail"
                  }
                }
                """.formatted(RUNNING_HOBBY_ID, OffsetDateTime.parse("2026-07-19T10:00:00Z"));

        mockMvc.perform(post("/api/sessions")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content(sessionPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Park Run"))
                .andExpect(jsonPath("$.location.placeId").value("place-abc"))
                .andExpect(jsonPath("$.location.label").value("Park Run Start"))
                .andExpect(jsonPath("$.location.name").doesNotExist())
                .andExpect(jsonPath("$.location.lat").doesNotExist())
                .andExpect(jsonPath("$.location.lng").doesNotExist());

        UUID sessionId = jdbcTemplate.queryForObject(
                "SELECT id FROM sessions WHERE user_id = ? AND title = ?",
                UUID.class,
                "test-user",
                "Park Run"
        );

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId)
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.placeId").value("place-abc"))
                .andExpect(jsonPath("$.location.label").value("Park Run Start"));

        Integer placeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM places WHERE place_id = ?",
                Integer.class,
                "place-abc"
        );
        String persistedPlaceId = jdbcTemplate.queryForObject(
                "SELECT place_id FROM sessions WHERE id = ?::uuid",
                String.class,
                sessionId.toString()
        );
        String locationLabel = jdbcTemplate.queryForObject(
                "SELECT location_label FROM sessions WHERE id = ?::uuid",
                String.class,
                sessionId.toString()
        );
        OffsetDateTime validatedAt = jdbcTemplate.queryForObject(
                "SELECT validated_at FROM places WHERE place_id = ?",
                OffsetDateTime.class,
                "place-abc"
        );

        assertThat(placeCount).isEqualTo(1);
        assertThat(persistedPlaceId).isEqualTo("place-abc");
        assertThat(locationLabel).isEqualTo("Park Run Start");
        assertThat(validatedAt).isNotNull();
    }

    private void insertUserWithRunningHobby(String userId, String email, String name) {
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, name, email_verified, bio, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                userId,
                email,
                name,
                true,
                null,
                OffsetDateTime.parse("2026-07-19T00:00:00Z")
        );
        jdbcTemplate.update(
                """
                INSERT INTO user_hobbies (user_id, hobby_id, experience_level)
                VALUES (?, ?::uuid, ?)
                """,
                userId,
                RUNNING_HOBBY_ID.toString(),
                "intermediate"
        );
    }
}
