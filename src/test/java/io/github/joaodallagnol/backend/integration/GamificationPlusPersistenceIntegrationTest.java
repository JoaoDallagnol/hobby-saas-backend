package io.github.joaodallagnol.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration(classes = {PostgresIntegrationTestSupport.AuthTestConfig.class})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "app.features.gamification=true",
        "app.features.plus-features=true"
})
class GamificationPlusPersistenceIntegrationTest extends PostgresIntegrationTestSupport {

    private static final UUID RUNNING_HOBBY_ID = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");

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
        jdbcTemplate.update("DELETE FROM sessions");
        jdbcTemplate.update("DELETE FROM equipment");
        jdbcTemplate.update("DELETE FROM backlog_items");
        jdbcTemplate.update("DELETE FROM user_hobbies");
        jdbcTemplate.update("DELETE FROM hobby_suggestions");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("""
                INSERT INTO users (id, email, name, email_verified, created_at)
                VALUES ('test-user', 'user@example.com', 'Example User', true, now())
                """);
        jdbcTemplate.update("""
                INSERT INTO user_hobbies (user_id, hobby_id, experience_level)
                VALUES ('test-user', ?::uuid, 'intermediate')
                """, RUNNING_HOBBY_ID.toString());
        insertSession(60, "2026-07-20T10:00:00Z");
        insertSession(30, "2026-07-21T10:00:00Z");
    }

    @AfterEach
    void clearGamificationDependencies() {
        jdbcTemplate.update("DELETE FROM user_featured_badges");
        jdbcTemplate.update("DELETE FROM user_badges");
        jdbcTemplate.update("DELETE FROM hobby_xp");
        jdbcTemplate.update("DELETE FROM goals");
        jdbcTemplate.update("DELETE FROM subscriptions");
        jdbcTemplate.update("DELETE FROM equipment_maintenance_rules");
    }

    @Test
    void persistsFreeGamificationAndEnforcesPlusFromDatabase() throws Exception {
        mockMvc.perform(get("/api/me/gamification").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hobbies[0].xp").value(38))
                .andExpect(jsonPath("$.records.longestSessionMinutes").value(60))
                .andExpect(jsonPath("$.badges[0].key").value("first_session"));

        mockMvc.perform(post("/api/me/goals")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"hobbyId":"%s","name":"Treinar","metric":"sessions",
                                 "targetValue":4,"cadence":"weekly"}
                                """.formatted(RUNNING_HOBBY_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progress").value(2))
                .andExpect(jsonPath("$.advanced").value(false));

        mockMvc.perform(get("/api/me/insights")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .param("from", "2026-07-20").param("to", "2026-07-21"))
                .andExpect(status().isForbidden());

        jdbcTemplate.update("""
                INSERT INTO subscriptions (user_id, plan, status, current_period_end)
                VALUES ('test-user', 'plus', 'active', now() + interval '30 days')
                """);

        mockMvc.perform(get("/api/me/insights")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .param("from", "2026-07-20").param("to", "2026-07-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.sessions").value(2))
                .andExpect(jsonPath("$.current.minutes").value(90));

        mockMvc.perform(post("/api/me/goals")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Julho ativo","metric":"minutes","targetValue":120,"cadence":"custom",
                                 "startDate":"2026-07-01","endDate":"2026-07-31"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.advanced").value(true))
                .andExpect(jsonPath("$.progress").value(90));

        mockMvc.perform(get("/api/me/wrapped")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .param("year", "2026").param("month", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.bestStreakDays").value(2))
                .andExpect(jsonPath("$.summary.dailyActivity.length()").value(2))
                .andExpect(jsonPath("$.badgesEarned[0].key").value("first_session"));

        mockMvc.perform(post("/api/me/backlog-items")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Exposição","status":"pending","dueDate":"2026-08-15",
                                 "priority":"high","position":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("high"))
                .andExpect(jsonPath("$.position").value(2));

        UUID equipmentId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO equipment (id, user_id, hobby_id, category, name)
                VALUES (?::uuid, 'test-user', ?::uuid, 'Shoes', 'Daily Trainer')
                """, equipmentId.toString(), RUNNING_HOBBY_ID.toString());
        jdbcTemplate.update("""
                INSERT INTO session_equipment (session_id, equipment_id)
                SELECT id, ?::uuid FROM sessions WHERE user_id = 'test-user'
                """, equipmentId.toString());
        mockMvc.perform(post("/api/me/equipment-maintenance")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"equipmentId":"%s","name":"Revisar","intervalMinutes":60,"active":true}
                                """.formatted(equipmentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usedMinutes").value(90))
                .andExpect(jsonPath("$.maintenanceDue").value(true));

        UUID badgeId = jdbcTemplate.queryForObject(
                "SELECT id FROM user_badges WHERE user_id = 'test-user' AND badge_key = 'first_session'",
                UUID.class);
        mockMvc.perform(patch("/api/me/profile-customization")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"theme":"midnight","featuredBadgeIds":["%s"]}
                                """.formatted(badgeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("midnight"))
                .andExpect(jsonPath("$.supporterBadge").value(true));

        jdbcTemplate.update("UPDATE subscriptions SET status = 'canceled' WHERE user_id = 'test-user'");
        mockMvc.perform(get("/api/me/profile-customization").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("midnight"))
                .andExpect(jsonPath("$.supporterBadge").value(false));
        mockMvc.perform(get("/api/me/backlog-items").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].priority").value("high"));
        mockMvc.perform(get("/api/me/equipment-maintenance").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Revisar"));
        mockMvc.perform(get("/api/me/insights")
                        .header(AUTHORIZATION, "Bearer valid-token")
                        .param("from", "2026-07-20").param("to", "2026-07-21"))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hobby_xp WHERE user_id = 'test-user'", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT profile_theme FROM users WHERE id = 'test-user'", String.class)).isEqualTo("midnight");
    }

    private void insertSession(int minutes, String startedAt) {
        jdbcTemplate.update("""
                INSERT INTO sessions
                    (id, user_id, hobby_id, title, started_at, duration_minutes, satisfaction)
                VALUES (?::uuid, 'test-user', ?::uuid, 'Practice', ?, ?, 4)
                """, UUID.randomUUID().toString(), RUNNING_HOBBY_ID.toString(), OffsetDateTime.parse(startedAt), minutes);
    }
}
