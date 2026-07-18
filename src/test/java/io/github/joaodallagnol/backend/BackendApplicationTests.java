package io.github.joaodallagnol.backend;

import io.github.joaodallagnol.backend.auth.FirebaseTokenVerifier;
import io.github.joaodallagnol.backend.auth.FirebaseVerifiedToken;
import io.github.joaodallagnol.backend.session.BacklogItemReferenceRepository;
import io.github.joaodallagnol.backend.session.EquipmentReferenceRepository;
import io.github.joaodallagnol.backend.session.HobbyAttributeTemplateRepository;
import io.github.joaodallagnol.backend.session.PlaceReferenceRepository;
import io.github.joaodallagnol.backend.session.SessionRecordRepository;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.ProductUserRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class TestBeans {

		@Bean
		@Primary
		ProductUserRepository productUserRepository() {
			return (ProductUserRepository) Proxy.newProxyInstance(
					ProductUserRepository.class.getClassLoader(),
					new Class<?>[]{ProductUserRepository.class},
					(proxy, method, args) -> switch (method.getName()) {
						case "findById" -> Optional.empty();
						case "save" -> args[0];
						case "equals" -> proxy == args[0];
						case "hashCode" -> System.identityHashCode(proxy);
						case "toString" -> "TestProductUserRepository";
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
						case "toString" -> "TestUserHobbyRepository";
						default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
					}
			);
		}

		@Bean
		@Primary
		HobbyRepository hobbyRepository() {
			return (HobbyRepository) Proxy.newProxyInstance(
					HobbyRepository.class.getClassLoader(),
					new Class<?>[]{HobbyRepository.class},
					(proxy, method, args) -> switch (method.getName()) {
						case "findById" -> Optional.empty();
						case "equals" -> proxy == args[0];
						case "hashCode" -> System.identityHashCode(proxy);
						case "toString" -> "TestHobbyRepository";
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
						case "findByIdAndUserId" -> Optional.empty();
						case "findAllByUserIdOrderByStartedAtDesc" -> List.of();
						case "findAllByUserIdAndHobbyIdOrderByStartedAtDesc" -> List.of();
						case "save" -> args[0];
						case "delete" -> null;
						case "equals" -> proxy == args[0];
						case "hashCode" -> System.identityHashCode(proxy);
						case "toString" -> "TestSessionRecordRepository";
						default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
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
						case "toString" -> "TestEquipmentReferenceRepository";
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
						case "toString" -> "TestBacklogItemReferenceRepository";
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
						case "equals" -> proxy == args[0];
						case "hashCode" -> System.identityHashCode(proxy);
						case "toString" -> "TestPlaceReferenceRepository";
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
						case "toString" -> "TestHobbyAttributeTemplateRepository";
						default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
					}
			);
		}

		@Bean
		@Primary
		FirebaseTokenVerifier testFirebaseTokenVerifier() {
			return idToken -> new FirebaseVerifiedToken("test-user", "user@example.com", "Example User", true);
		}
	}

}
