package io.github.joaodallagnol.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.joaodallagnol.backend.storage.SessionPhotoStorageKeyPolicy;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {PostgresIntegrationTestSupport.AuthTestConfig.class})
class SessionPhotoPersistenceIntegrationTest extends PostgresIntegrationTestSupport {

    private static final UUID RUNNING_HOBBY_ID =
            UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
    private static final String USER_ID = "test-user";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM user_featured_badges");
        jdbcTemplate.update("DELETE FROM user_badges");
        jdbcTemplate.update("DELETE FROM hobby_xp");
        jdbcTemplate.update("DELETE FROM goals");
        jdbcTemplate.update("DELETE FROM subscriptions");
        jdbcTemplate.update("DELETE FROM equipment_maintenance_rules");
        jdbcTemplate.update("DELETE FROM session_equipment");
        jdbcTemplate.update("DELETE FROM session_photos");
        jdbcTemplate.update("DELETE FROM photo_storage_deletions");
        jdbcTemplate.update("DELETE FROM sessions");
        jdbcTemplate.update("DELETE FROM equipment");
        jdbcTemplate.update("DELETE FROM backlog_items");
        jdbcTemplate.update("DELETE FROM user_hobbies");
        jdbcTemplate.update("DELETE FROM hobby_suggestions");
        jdbcTemplate.update("DELETE FROM places");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, name, email_verified, created_at)
                VALUES ('test-user', 'user@example.com', 'Example User', true, now())
                """);
        jdbcTemplate.update("""
                INSERT INTO user_hobbies (user_id, hobby_id, experience_level)
                VALUES ('test-user', ?::uuid, 'intermediate')
                """, RUNNING_HOBBY_ID.toString());
    }

    @Test
    void keepsPhotoLinkedAcrossCreateListDetailPreserveReplaceAndRemove() throws Exception {
        String firstStorageKey = uploadKey("first.jpg");
        mockMvc.perform(post("/api/sessions")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content(sessionPayload("Photo flow", "[{\"storageKey\":\"" + firstStorageKey + "\"}]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photos.length()").value(1))
                .andExpect(jsonPath("$.photos[0].processingStatus").value("pending"))
                .andExpect(jsonPath("$.photos[0].deliveryStatus").value("processing"))
                .andExpect(jsonPath("$.photos[0].originalUrl").isEmpty())
                .andExpect(jsonPath("$.photos[0].thumbnailUrl").isEmpty());

        UUID sessionId = jdbcTemplate.queryForObject(
                "SELECT id FROM sessions WHERE user_id = ? AND title = ?",
                UUID.class,
                USER_ID,
                "Photo flow"
        );
        UUID firstPhotoId = jdbcTemplate.queryForObject(
                "SELECT id FROM session_photos WHERE session_id = ?::uuid",
                UUID.class,
                sessionId.toString()
        );

        mockMvc.perform(get("/api/sessions")
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].photos[0].id").value(firstPhotoId.toString()));
        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId)
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos[0].id").value(firstPhotoId.toString()));

        mockMvc.perform(patch("/api/sessions/{sessionId}", sessionId)
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content(sessionPayload(
                                "Photo preserved",
                                "[{\"id\":\"" + firstPhotoId + "\"}]"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos[0].id").value(firstPhotoId.toString()));

        String replacementStorageKey = uploadKey("replacement.jpg");
        mockMvc.perform(patch("/api/sessions/{sessionId}", sessionId)
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content(sessionPayload(
                                "Photo replaced",
                                "[{\"storageKey\":\"" + replacementStorageKey + "\"}]"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos.length()").value(1))
                .andExpect(jsonPath("$.photos[0].id").value(
                        org.hamcrest.Matchers.not(firstPhotoId.toString())
                ));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_photos WHERE session_id = ?::uuid",
                Integer.class,
                sessionId.toString()
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT storage_key_original FROM session_photos WHERE session_id = ?::uuid",
                String.class,
                sessionId.toString()
        )).isEqualTo(replacementStorageKey);
        assertThat(jdbcTemplate.queryForList(
                "SELECT storage_key FROM photo_storage_deletions",
                String.class
        )).contains(firstStorageKey);

        mockMvc.perform(patch("/api/sessions/{sessionId}", sessionId)
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content(sessionPayload("Photo removed", "[]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos").isEmpty());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_photos WHERE session_id = ?::uuid",
                Integer.class,
                sessionId.toString()
        )).isZero();
        assertThat(jdbcTemplate.queryForList(
                "SELECT storage_key FROM photo_storage_deletions",
                String.class
        )).contains(firstStorageKey, replacementStorageKey);
    }

    private String uploadKey(String fileName) {
        return SessionPhotoStorageKeyPolicy.uploadPrefix(USER_ID) + "2026/07/30/" + fileName;
    }

    private String sessionPayload(String title, String photosJson) {
        return """
                {
                  "hobbyId": "%s",
                  "title": "%s",
                  "startedAt": "2026-07-30T20:00:00Z",
                  "durationMinutes": 30,
                  "notes": null,
                  "satisfaction": 4,
                  "visibility": "only_me",
                  "location": null,
                  "projectId": null,
                  "equipmentIds": [],
                  "photos": %s,
                  "attributes": {}
                }
                """.formatted(RUNNING_HOBBY_ID, title, photosJson);
    }
}
