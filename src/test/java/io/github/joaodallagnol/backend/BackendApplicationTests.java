package io.github.joaodallagnol.backend;

import io.github.joaodallagnol.backend.auth.FirebaseTokenVerifier;
import io.github.joaodallagnol.backend.auth.FirebaseVerifiedToken;
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
		FirebaseTokenVerifier testFirebaseTokenVerifier() {
			return idToken -> new FirebaseVerifiedToken("test-user", "user@example.com", "Example User", true);
		}
	}

}
