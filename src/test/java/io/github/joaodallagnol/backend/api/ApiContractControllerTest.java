package io.github.joaodallagnol.backend.api;
import io.github.joaodallagnol.backend.auth.FirebaseTokenVerifier;
import io.github.joaodallagnol.backend.auth.FirebaseVerifiedToken;
import io.github.joaodallagnol.backend.auth.FirebaseAuthenticationFilter;
import io.github.joaodallagnol.backend.config.RateLimitFilter;
import io.github.joaodallagnol.backend.backlog.BacklogController;
import io.github.joaodallagnol.backend.backlog.BacklogItemResponse;
import io.github.joaodallagnol.backend.backlog.BacklogService;
import io.github.joaodallagnol.backend.config.ApiExceptionHandler;
import io.github.joaodallagnol.backend.equipment.EquipmentController;
import io.github.joaodallagnol.backend.equipment.EquipmentResponse;
import io.github.joaodallagnol.backend.equipment.EquipmentService;
import io.github.joaodallagnol.backend.session.HobbyAttributeTemplateController;
import io.github.joaodallagnol.backend.session.HobbyAttributeTemplateResponse;
import io.github.joaodallagnol.backend.session.HobbyAttributeTemplateService;
import io.github.joaodallagnol.backend.session.SessionController;
import io.github.joaodallagnol.backend.session.SessionLocationResponse;
import io.github.joaodallagnol.backend.session.SessionPageResponse;
import io.github.joaodallagnol.backend.session.SessionPhotoResponse;
import io.github.joaodallagnol.backend.session.SessionResponse;
import io.github.joaodallagnol.backend.session.SessionService;
import io.github.joaodallagnol.backend.storage.SessionPhotoUploadController;
import io.github.joaodallagnol.backend.storage.SessionPhotoUploadResponse;
import io.github.joaodallagnol.backend.storage.SessionPhotoUploadService;
import io.github.joaodallagnol.backend.streak.StreakController;
import io.github.joaodallagnol.backend.streak.StreakResponse;
import io.github.joaodallagnol.backend.streak.StreakService;
import io.github.joaodallagnol.backend.user.CurrentUserController;
import io.github.joaodallagnol.backend.user.CurrentUserProfileResponse;
import io.github.joaodallagnol.backend.user.CurrentUserProfileService;
import io.github.joaodallagnol.backend.user.UserHobbyResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import io.github.joaodallagnol.backend.user.JitUserProvisioningFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        CurrentUserController.class,
        EquipmentController.class,
        BacklogController.class,
        SessionPhotoUploadController.class,
        StreakController.class,
        HobbyAttributeTemplateController.class,
        SessionController.class
}, excludeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FirebaseAuthenticationFilter.class),
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JitUserProvisioningFilter.class),
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitFilter.class)
})
@AutoConfigureMockMvc(addFilters = false)
@Import({ApiExceptionHandler.class, ApiContractControllerTest.MockServicesConfig.class})
class ApiContractControllerTest {

    private static final UUID HOBBY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EQUIPMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BACKLOG_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SESSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID TEMPLATE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StubCurrentUserProfileService currentUserProfileService;

    @Autowired
    private StubEquipmentService equipmentService;

    @Autowired
    private StubBacklogService backlogService;

    @Autowired
    private StubSessionPhotoUploadService sessionPhotoUploadService;

    @Autowired
    private StubStreakService streakService;

    @Autowired
    private StubHobbyAttributeTemplateService hobbyAttributeTemplateService;

    @Autowired
    private StubSessionService sessionService;

    @Test
    void shouldExposeCurrentUserProfileContract() throws Exception {
        currentUserProfileService.currentUserProfile = new CurrentUserProfileResponse(
                "firebase-user-1",
                "user@example.com",
                "Example User",
                true,
                "Runner and reader",
                OffsetDateTime.parse("2026-07-19T12:00:00Z")
        );

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("firebase-user-1"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("Example User"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.bio").value("Runner and reader"))
                .andExpect(jsonPath("$.createdAt").value("2026-07-19T12:00:00Z"));
    }

    @Test
    void shouldReturnValidationProblemForInvalidProfileUpdate() throws Exception {
        mockMvc.perform(patch("/api/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "bio": "ok"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.detail").value("Request validation failed."))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void shouldExposeCurrentUserHobbiesContract() throws Exception {
        currentUserProfileService.currentUserHobbies = List.of(
                new UserHobbyResponse(HOBBY_ID, "Running", "Sports & Movement", "shoe", "intermediate")
        );

        mockMvc.perform(get("/api/me/hobbies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hobbyId").value(HOBBY_ID.toString()))
                .andExpect(jsonPath("$[0].hobbyName").value("Running"))
                .andExpect(jsonPath("$[0].categoryName").value("Sports & Movement"))
                .andExpect(jsonPath("$[0].icon").value("shoe"))
                .andExpect(jsonPath("$[0].experienceLevel").value("intermediate"));
    }

    @Test
    void shouldExposeEquipmentListContract() throws Exception {
        equipmentService.equipmentResponses = List.of(
                new EquipmentResponse(EQUIPMENT_ID, HOBBY_ID, "Running", "Shoes", "Daily Trainer")
        );

        mockMvc.perform(get("/api/me/equipment").param("hobbyId", HOBBY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(EQUIPMENT_ID.toString()))
                .andExpect(jsonPath("$[0].hobbyId").value(HOBBY_ID.toString()))
                .andExpect(jsonPath("$[0].hobbyName").value("Running"))
                .andExpect(jsonPath("$[0].category").value("Shoes"))
                .andExpect(jsonPath("$[0].name").value("Daily Trainer"));
    }

    @Test
    void shouldExposeBacklogListContract() throws Exception {
        backlogService.backlogItems = List.of(
                new BacklogItemResponse(
                        BACKLOG_ID,
                        HOBBY_ID,
                        "Running",
                        "Half marathon plan",
                        "pending",
                        OffsetDateTime.parse("2026-07-19T12:30:00Z")
                )
        );

        mockMvc.perform(get("/api/me/backlog-items").param("hobbyId", HOBBY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(BACKLOG_ID.toString()))
                .andExpect(jsonPath("$[0].hobbyId").value(HOBBY_ID.toString()))
                .andExpect(jsonPath("$[0].hobbyName").value("Running"))
                .andExpect(jsonPath("$[0].title").value("Half marathon plan"))
                .andExpect(jsonPath("$[0].status").value("pending"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-07-19T12:30:00Z"));
    }

    @Test
    void shouldExposePhotoUploadContract() throws Exception {
        sessionPhotoUploadService.uploadResponse = new SessionPhotoUploadResponse(
                "users/firebase-user-1/sessions/tmp/photo.webp",
                "https://example.r2.dev/upload",
                "PUT",
                Map.of("Content-Type", "image/webp"),
                OffsetDateTime.parse("2026-07-19T13:00:00Z")
        );

        mockMvc.perform(post("/api/me/session-photos/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentType": "image/webp",
                                  "fileName": "photo.webp",
                                  "sizeBytes": 123456
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storageKey").value("users/firebase-user-1/sessions/tmp/photo.webp"))
                .andExpect(jsonPath("$.uploadUrl").value("https://example.r2.dev/upload"))
                .andExpect(jsonPath("$.method").value("PUT"))
                .andExpect(jsonPath("$.requiredHeaders.Content-Type").value("image/webp"))
                .andExpect(jsonPath("$.expiresAt").value("2026-07-19T13:00:00Z"));
    }

    @Test
    void shouldExposeStreakContract() throws Exception {
        streakService.streakResponse = new StreakResponse(
                7,
                LocalDate.parse("2026-07-18"),
                LocalDate.parse("2026-07-19"),
                false
        );

        mockMvc.perform(get("/api/me/streak"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreakDays").value(7))
                .andExpect(jsonPath("$.lastActiveDateUtc").value("2026-07-18"))
                .andExpect(jsonPath("$.referenceDateUtc").value("2026-07-19"))
                .andExpect(jsonPath("$.activeToday").value(false));
    }

    @Test
    void shouldExposeAttributeTemplateContract() throws Exception {
        hobbyAttributeTemplateService.templateResponses = List.of(
                new HobbyAttributeTemplateResponse(TEMPLATE_ID, "distance_km", "Distance", "number", "km", 1)
        );

        mockMvc.perform(get("/api/hobbies/{hobbyId}/attribute-templates", HOBBY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TEMPLATE_ID.toString()))
                .andExpect(jsonPath("$[0].key").value("distance_km"))
                .andExpect(jsonPath("$[0].label").value("Distance"))
                .andExpect(jsonPath("$[0].type").value("number"))
                .andExpect(jsonPath("$[0].unit").value("km"))
                .andExpect(jsonPath("$[0].displayOrder").value(1));
    }

    @Test
    void shouldExposeSessionDetailContract() throws Exception {
        sessionService.sessionResponse = new SessionResponse(
                SESSION_ID,
                HOBBY_ID,
                "Running",
                "Morning Run",
                OffsetDateTime.parse("2026-07-19T07:30:00Z"),
                45,
                "Good pace",
                4,
                new SessionLocationResponse(
                        "place-123",
                        "Ibirapuera Park"
                ),
                BACKLOG_ID,
                List.of(EQUIPMENT_ID),
                List.of(new SessionPhotoResponse(UUID.randomUUID(), "uploads/original.webp", "uploads/thumb.webp", "ready")),
                Map.of("distance_km", 8.5, "surface", "road")
        );

        mockMvc.perform(get("/api/sessions/{sessionId}", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.hobbyId").value(HOBBY_ID.toString()))
                .andExpect(jsonPath("$.hobbyName").value("Running"))
                .andExpect(jsonPath("$.title").value("Morning Run"))
                .andExpect(jsonPath("$.startedAt").value("2026-07-19T07:30:00Z"))
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.notes").value("Good pace"))
                .andExpect(jsonPath("$.satisfaction").value(4))
                .andExpect(jsonPath("$.location.placeId").value("place-123"))
                .andExpect(jsonPath("$.location.label").value("Ibirapuera Park"))
                .andExpect(jsonPath("$.location.name").doesNotExist())
                .andExpect(jsonPath("$.location.lat").doesNotExist())
                .andExpect(jsonPath("$.location.lng").doesNotExist())
                .andExpect(jsonPath("$.projectId").value(BACKLOG_ID.toString()))
                .andExpect(jsonPath("$.equipmentIds[0]").value(EQUIPMENT_ID.toString()))
                .andExpect(jsonPath("$.photos[0].id").isNotEmpty())
                .andExpect(jsonPath("$.visibility").value("only_me"))
                .andExpect(jsonPath("$.photos[0].originalUrl").value("uploads/original.webp"))
                .andExpect(jsonPath("$.photos[0].thumbnailUrl").value("uploads/thumb.webp"))
                .andExpect(jsonPath("$.photos[0].processingStatus").value("ready"))
                .andExpect(jsonPath("$.attributes.distance_km").value(8.5))
                .andExpect(jsonPath("$.attributes.surface").value("road"));
    }

    @Test
    void shouldExposePaginatedSessionListContract() throws Exception {
        sessionService.sessionPageResponse = new SessionPageResponse(
                List.of(new SessionResponse(
                        SESSION_ID,
                        HOBBY_ID,
                        "Running",
                        "Morning Run",
                        OffsetDateTime.parse("2026-07-19T07:30:00Z"),
                        45,
                        null,
                        4,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        Map.of("distance_km", 8.5)
                )),
                0,
                20,
                1,
                1,
                false
        );

        mockMvc.perform(get("/api/sessions").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void shouldExposeSessionCreateContract() throws Exception {
        sessionService.sessionResponse = new SessionResponse(
                SESSION_ID,
                HOBBY_ID,
                "Running",
                "Long Run",
                OffsetDateTime.parse("2026-07-19T09:00:00Z"),
                75,
                "Strong pace throughout the route.",
                5,
                new SessionLocationResponse("place-123", "Ibirapuera Park"),
                BACKLOG_ID,
                List.of(EQUIPMENT_ID),
                List.of(),
                Map.of("distance_km", 14.2)
        );

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "hobbyId": "%s",
                                  "title": "Long Run",
                                  "startedAt": "2026-07-19T09:00:00Z",
                                  "durationMinutes": 75,
                                  "notes": "Strong pace throughout the route.",
                                  "satisfaction": 5,
                                  "location": {
                                    "placeId": "place-123",
                                    "label": "Ibirapuera Park"
                                  },
                                  "projectId": "%s",
                                  "equipmentIds": ["%s"],
                                  "attributes": {
                                    "distance_km": 14.2
                                  }
                                }
                                """.formatted(HOBBY_ID, BACKLOG_ID, EQUIPMENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$.title").value("Long Run"))
                .andExpect(jsonPath("$.location.placeId").value("place-123"))
                .andExpect(jsonPath("$.projectId").value(BACKLOG_ID.toString()))
                .andExpect(jsonPath("$.equipmentIds[0]").value(EQUIPMENT_ID.toString()))
                .andExpect(jsonPath("$.attributes.distance_km").value(14.2));
    }

    @TestConfiguration
    static class MockServicesConfig {

        @Bean
        StubCurrentUserProfileService currentUserProfileService() {
            return new StubCurrentUserProfileService();
        }

        @Bean
        StubEquipmentService equipmentService() {
            return new StubEquipmentService();
        }

        @Bean
        StubBacklogService backlogService() {
            return new StubBacklogService();
        }

        @Bean
        StubSessionPhotoUploadService sessionPhotoUploadService() {
            return new StubSessionPhotoUploadService();
        }

        @Bean
        StubStreakService streakService() {
            return new StubStreakService();
        }

        @Bean
        StubHobbyAttributeTemplateService hobbyAttributeTemplateService() {
            return new StubHobbyAttributeTemplateService();
        }

        @Bean
        StubSessionService sessionService() {
            return new StubSessionService();
        }

        @Bean
        FirebaseTokenVerifier firebaseTokenVerifier() {
            return idToken -> new FirebaseVerifiedToken("test-user", "user@example.com", "Example User", true);
        }
    }

    static final class StubCurrentUserProfileService extends CurrentUserProfileService {

        private CurrentUserProfileResponse currentUserProfile;
        private List<UserHobbyResponse> currentUserHobbies = List.of();

        StubCurrentUserProfileService() {
            super(null, null, null, null);
        }

        @Override
        public CurrentUserProfileResponse getCurrentUserProfile() {
            return currentUserProfile;
        }

        @Override
        public List<UserHobbyResponse> getCurrentUserHobbies() {
            return currentUserHobbies;
        }
    }

    static final class StubEquipmentService extends EquipmentService {

        private List<EquipmentResponse> equipmentResponses = List.of();

        StubEquipmentService() {
            super(null, null, null, null);
        }

        @Override
        public List<EquipmentResponse> listEquipment(UUID hobbyId) {
            return equipmentResponses;
        }
    }

    static final class StubBacklogService extends BacklogService {

        private List<BacklogItemResponse> backlogItems = List.of();

        StubBacklogService() {
            super(null, null, null, null);
        }

        @Override
        public List<BacklogItemResponse> listItems(UUID hobbyId) {
            return backlogItems;
        }
    }

    static final class StubSessionPhotoUploadService extends SessionPhotoUploadService {

        private SessionPhotoUploadResponse uploadResponse;

        StubSessionPhotoUploadService() {
            super(null, null, null);
        }

        @Override
        public SessionPhotoUploadResponse createUpload(io.github.joaodallagnol.backend.storage.CreateSessionPhotoUploadRequest request) {
            return uploadResponse;
        }
    }

    static final class StubStreakService extends StreakService {

        private StreakResponse streakResponse;

        StubStreakService() {
            super(null, null, null);
        }

        @Override
        public StreakResponse getCurrentUserStreak() {
            return streakResponse;
        }
    }

    static final class StubHobbyAttributeTemplateService extends HobbyAttributeTemplateService {

        private List<HobbyAttributeTemplateResponse> templateResponses = List.of();

        StubHobbyAttributeTemplateService() {
            super(null, null, null, (io.github.joaodallagnol.backend.session.HobbyAttributeTemplateCatalog) null);
        }

        @Override
        public List<HobbyAttributeTemplateResponse> listTemplates(UUID hobbyId) {
            return templateResponses;
        }
    }

    static final class StubSessionService extends SessionService {

        private SessionResponse sessionResponse;
        private SessionPageResponse sessionPageResponse;

        StubSessionService() {
            super(null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public SessionResponse createSession(io.github.joaodallagnol.backend.session.CreateSessionRequest request) {
            return sessionResponse;
        }

        @Override
        public SessionResponse getSession(UUID sessionId) {
            return sessionResponse;
        }

        @Override
        public SessionPageResponse listSessions(UUID hobbyId, int page, int size) {
            return sessionPageResponse;
        }
    }
}
