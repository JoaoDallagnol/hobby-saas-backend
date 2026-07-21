package io.github.joaodallagnol.backend.security;

import io.github.joaodallagnol.backend.auth.FirebaseTokenVerifier;
import io.github.joaodallagnol.backend.auth.FirebaseVerifiedToken;
import io.github.joaodallagnol.backend.session.BacklogItemReferenceRepository;
import io.github.joaodallagnol.backend.session.EquipmentReferenceRepository;
import io.github.joaodallagnol.backend.session.GooglePlaceDetailsClient;
import io.github.joaodallagnol.backend.session.HobbyAttributeTemplateRepository;
import io.github.joaodallagnol.backend.session.PlaceReferenceRepository;
import io.github.joaodallagnol.backend.session.ResolvedPlace;
import io.github.joaodallagnol.backend.session.SessionRecord;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.session.SessionPhotoRepository;
import io.github.joaodallagnol.backend.storage.PhotoStorageDeletionRepository;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.ProductUser;
import io.github.joaodallagnol.backend.user.ProductUserRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.joaodallagnol.backend.config.GamificationTestRepositoryConfig;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(GamificationTestRepositoryConfig.class)
class SecurityIntegrationTest {

    private static final UUID OWN_SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_SESSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID HOBBY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void protectedEndpointWithoutBearerReturns401() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}", OWN_SESSION_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithInvalidBearerReturns401AndErrorPayload() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}", OWN_SESSION_ID)
                        .header(AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.message").value("Bearer token is invalid."));
    }

    @Test
    void authenticatedUserCanReadOwnSession() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}", OWN_SESSION_ID)
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OWN_SESSION_ID.toString()))
                .andExpect(jsonPath("$.title").value("Morning Run"))
                .andExpect(jsonPath("$.hobbyId").value(HOBBY_ID.toString()))
                .andExpect(jsonPath("$.hobbyName").value("Running"));
    }

    @Test
    void authenticatedUserCannotReadSessionOwnedByAnotherUser() throws Exception {
        mockMvc.perform(get("/api/sessions/{sessionId}", OTHER_USER_SESSION_ID)
                        .header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Session not found."));
    }

    @TestConfiguration
    static class TestBeans {

        private final Map<String, ProductUser> users = new ConcurrentHashMap<>();
        private final Map<UUID, SessionRecord> sessions = Map.of(
                OWN_SESSION_ID, createSession(OWN_SESSION_ID, "test-user", "Morning Run"),
                OTHER_USER_SESSION_ID, createSession(OTHER_USER_SESSION_ID, "other-user", "Private Ride")
        );

        @Bean
        @Primary
        ProductUserRepository productUserRepository() {
            return (ProductUserRepository) Proxy.newProxyInstance(
                    ProductUserRepository.class.getClassLoader(),
                    new Class<?>[]{ProductUserRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findById" -> Optional.ofNullable(users.get((String) args[0]));
                        case "save" -> {
                            ProductUser user = (ProductUser) args[0];
                            users.put(user.getId(), user);
                            yield user;
                        }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SecurityTestProductUserRepository";
                        default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                    }
            );
        }

        @Bean
        @Primary
        UserHobbyRepository userHobbyRepository() {
            return (UserHobbyRepository) Proxy.newProxyInstance(
                    UserHobbyRepository.class.getClassLoader(),
                    new Class<?>[]{UserHobbyRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findAllByIdUserIdOrderByHobbyNameAsc" -> List.of();
                        case "findByIdUserIdAndIdHobbyId" -> Optional.empty();
                        case "existsByIdUserIdAndIdHobbyId" -> false;
                        case "save" -> args[0];
                        case "delete" -> null;
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SecurityTestUserHobbyRepository";
                        default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                    }
            );
        }

        @Bean
        @Primary
        HobbyRepository hobbyRepository() {
            Hobby hobby = createHobby();
            return (HobbyRepository) Proxy.newProxyInstance(
                    HobbyRepository.class.getClassLoader(),
                    new Class<?>[]{HobbyRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findById" -> HOBBY_ID.equals(args[0]) ? Optional.of(hobby) : Optional.empty();
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SecurityTestHobbyRepository";
                        default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                    }
            );
        }

        @Bean
        @Primary
        SessionRecordRepository sessionRecordRepository() {
            return (SessionRecordRepository) Proxy.newProxyInstance(
                    SessionRecordRepository.class.getClassLoader(),
                    new Class<?>[]{SessionRecordRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByIdAndUserId" -> {
                            UUID sessionId = (UUID) args[0];
                            String userId = (String) args[1];
                            SessionRecord session = sessions.get(sessionId);
                            yield session != null && userId.equals(session.getUserId()) ? Optional.of(session) : Optional.empty();
                        }
                        case "findAllByUserIdOrderByStartedAtDesc" -> sessions.values().stream()
                                .filter(session -> args[0].equals(session.getUserId()))
                                .sorted(Comparator.comparing(SessionRecord::getStartedAt).reversed())
                                .toList();
                        case "findAllByUserIdAndHobbyIdOrderByStartedAtDesc" -> sessions.values().stream()
                                .filter(session -> args[0].equals(session.getUserId()))
                                .filter(session -> args[1].equals(session.getHobby().getId()))
                                .sorted(Comparator.comparing(SessionRecord::getStartedAt).reversed())
                                .toList();
                        case "save" -> args[0];
                        case "delete" -> null;
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SecurityTestSessionRecordRepository";
                        default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                    }
            );
        }

        @Bean
        @Primary
        SessionPhotoRepository sessionPhotoRepository() {
            return (SessionPhotoRepository) Proxy.newProxyInstance(
                    SessionPhotoRepository.class.getClassLoader(),
                    new Class<?>[]{SessionPhotoRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findTop10ByProcessingStatusAndProcessingAttemptsLessThanOrderByIdAsc" -> List.of();
                        case "findById" -> Optional.empty();
                        case "save" -> args[0];
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SecurityTestSessionPhotoRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        @Bean
        @Primary
        PhotoStorageDeletionRepository photoStorageDeletionRepository() {
            return (PhotoStorageDeletionRepository) Proxy.newProxyInstance(
                    PhotoStorageDeletionRepository.class.getClassLoader(),
                    new Class<?>[]{PhotoStorageDeletionRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findTop20ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc" -> List.of();
                        case "save" -> args[0];
                        case "delete" -> null;
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SecurityTestPhotoStorageDeletionRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        @Bean
        @Primary
        EquipmentReferenceRepository equipmentReferenceRepository() {
            return (EquipmentReferenceRepository) Proxy.newProxyInstance(
                    EquipmentReferenceRepository.class.getClassLoader(),
                    new Class<?>[]{EquipmentReferenceRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findAllByIdInAndUserId" -> List.of();
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SecurityTestEquipmentReferenceRepository";
                        default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                    }
            );
        }

        @Bean
        @Primary
        BacklogItemReferenceRepository backlogItemReferenceRepository() {
            return (BacklogItemReferenceRepository) Proxy.newProxyInstance(
                    BacklogItemReferenceRepository.class.getClassLoader(),
                    new Class<?>[]{BacklogItemReferenceRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "existsByIdAndUserId" -> false;
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SecurityTestBacklogItemReferenceRepository";
                        default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                    }
            );
        }

        @Bean
        @Primary
        PlaceReferenceRepository placeReferenceRepository() {
            return (PlaceReferenceRepository) Proxy.newProxyInstance(
                    PlaceReferenceRepository.class.getClassLoader(),
                    new Class<?>[]{PlaceReferenceRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "existsById" -> false;
                        case "findById" -> Optional.empty();
                        case "save" -> args[0];
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SecurityTestPlaceReferenceRepository";
                        default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                    }
            );
        }

        @Bean
        @Primary
        HobbyAttributeTemplateRepository hobbyAttributeTemplateRepository() {
            return (HobbyAttributeTemplateRepository) Proxy.newProxyInstance(
                    HobbyAttributeTemplateRepository.class.getClassLoader(),
                    new Class<?>[]{HobbyAttributeTemplateRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findAllByHobbyIdOrderByDisplayOrderAsc" -> List.of();
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "SecurityTestHobbyAttributeTemplateRepository";
                        default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                    }
            );
        }

        @Bean
        @Primary
        GooglePlaceDetailsClient googlePlaceDetailsClient() {
            return placeId -> new ResolvedPlace(
                    placeId,
                    "Test Place",
                    BigDecimal.valueOf(-23.550520),
                    BigDecimal.valueOf(-46.633308)
            );
        }

        @Bean
        @Primary
        FirebaseTokenVerifier firebaseTokenVerifier() {
            return idToken -> switch (idToken) {
                case "valid-token" -> new FirebaseVerifiedToken("test-user", "user@example.com", "Example User", true);
                case "other-user-token" -> new FirebaseVerifiedToken("other-user", "other@example.com", "Other User", true);
                default -> throw new IllegalArgumentException("Token is invalid.");
            };
        }

        private static SessionRecord createSession(UUID sessionId, String userId, String title) {
            SessionRecord session = new SessionRecord(
                    userId,
                    createHobby(),
                    title,
                    OffsetDateTime.parse("2026-07-19T07:00:00Z"),
                    45,
                    "Session notes",
                    4,
                    null,
                    null,
                    Map.of()
            );
            ReflectionTestUtils.setField(session, "id", sessionId);
            return session;
        }

        private static Hobby createHobby() {
            Hobby hobby = BeanUtils.instantiateClass(Hobby.class);
            ReflectionTestUtils.setField(hobby, "id", HOBBY_ID);
            ReflectionTestUtils.setField(hobby, "name", "Running");
            return hobby;
        }
    }
}
