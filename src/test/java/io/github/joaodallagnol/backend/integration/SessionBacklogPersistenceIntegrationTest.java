package io.github.joaodallagnol.backend.integration;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {PostgresIntegrationTestSupport.AuthTestConfig.class})
class SessionBacklogPersistenceIntegrationTest extends PostgresIntegrationTestSupport {

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
        insertUserWithRunningHobby("other-user", "other@example.com", "Other User");
    }

    @Test
    void createsBacklogItemAndSessionLinkedToItUsingRealPostgres() throws Exception {
        String backlogPayload = """
                {
                  "hobbyId": "%s",
                  "title": "Half marathon plan",
                  "status": "pending"
                }
                """.formatted(RUNNING_HOBBY_ID);

        mockMvc.perform(post("/api/me/backlog-items")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content(backlogPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Half marathon plan"))
                .andExpect(jsonPath("$.status").value("pending"));

        UUID backlogId = jdbcTemplate.queryForObject(
                "SELECT id FROM backlog_items WHERE user_id = ? AND title = ?",
                UUID.class,
                "test-user",
                "Half marathon plan"
        );

        String sessionPayload = """
                {
                  "hobbyId": "%s",
                  "title": "Long run",
                  "startedAt": "%s",
                  "durationMinutes": 75,
                  "notes": "Strong pace throughout the route.",
                  "satisfaction": 5,
                  "projectId": "%s",
                  "attributes": {
                    "distance_km": 14.2,
                    "avg_pace_min_km": 5.22,
                    "surface": "road"
                  }
                }
                """.formatted(
                RUNNING_HOBBY_ID,
                OffsetDateTime.parse("2026-07-19T09:00:00Z"),
                backlogId
        );

        mockMvc.perform(post("/api/sessions")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content(sessionPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Long run"))
                .andExpect(jsonPath("$.projectId").value(backlogId.toString()))
                .andExpect(jsonPath("$.attributes.distance_km").value(14.2));

        UUID sessionId = jdbcTemplate.queryForObject(
                "SELECT id FROM sessions WHERE user_id = ? AND title = ?",
                UUID.class,
                "test-user",
                "Long run"
        );

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId)
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.projectId").value(backlogId.toString()))
                .andExpect(jsonPath("$.attributes.surface").value("road"));

        mockMvc.perform(get("/api/me/backlog-items")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(backlogId.toString()))
                .andExpect(jsonPath("$[0].title").value("Half marathon plan"));

        Integer backlogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM backlog_items WHERE user_id = ?",
                Integer.class,
                "test-user"
        );
        Integer sessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE user_id = ? AND project_id = ?::uuid",
                Integer.class,
                "test-user",
                backlogId.toString()
        );
        String persistedTitle = jdbcTemplate.queryForObject(
                "SELECT title FROM sessions WHERE id = ?::uuid",
                String.class,
                sessionId.toString()
        );

        assertThat(backlogCount).isEqualTo(1);
        assertThat(sessionCount).isEqualTo(1);
        assertThat(persistedTitle).isEqualTo("Long run");
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
