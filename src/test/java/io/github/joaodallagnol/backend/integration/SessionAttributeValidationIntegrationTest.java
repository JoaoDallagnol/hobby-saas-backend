package io.github.joaodallagnol.backend.integration;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {PostgresIntegrationTestSupport.AuthTestConfig.class})
class SessionAttributeValidationIntegrationTest extends PostgresIntegrationTestSupport {

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
        jdbcTemplate.update("DELETE FROM users");

        insertUserWithRunningHobby("test-user", "user@example.com", "Example User");
    }

    @Test
    void rejectsUnknownDynamicAttributeKey() throws Exception {
        String payload = """
                {
                  "hobbyId": "%s",
                  "title": "Morning Run",
                  "startedAt": "%s",
                  "durationMinutes": 45,
                  "satisfaction": 4,
                  "attributes": {
                    "pages_read": 12
                  }
                }
                """.formatted(RUNNING_HOBBY_ID, OffsetDateTime.parse("2026-07-19T07:30:00Z"));

                mockMvc.perform(post("/api/sessions")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.title").value("Invalid request"))
                        .andExpect(jsonPath("$.detail").value("Attribute key is not allowed for the selected hobby: pages_read"));
    }

    @Test
    void rejectsDynamicAttributeTypeMismatch() throws Exception {
        String payload = """
                {
                  "hobbyId": "%s",
                  "title": "Morning Run",
                  "startedAt": "%s",
                  "durationMinutes": 45,
                  "satisfaction": 4,
                  "attributes": {
                    "distance_km": "far"
                  }
                }
                """.formatted(RUNNING_HOBBY_ID, OffsetDateTime.parse("2026-07-19T07:30:00Z"));

                mockMvc.perform(post("/api/sessions")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.title").value("Invalid request"))
                        .andExpect(jsonPath("$.detail").value("Attribute value has invalid type for key: distance_km"));
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
