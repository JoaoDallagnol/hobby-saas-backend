package io.github.joaodallagnol.backend;

import io.github.joaodallagnol.backend.auth.FirebaseTokenVerifier;
import io.github.joaodallagnol.backend.auth.FirebaseVerifiedToken;
import io.github.joaodallagnol.backend.user.ProductUserRepository;
import java.lang.reflect.Proxy;
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
		FirebaseTokenVerifier testFirebaseTokenVerifier() {
			return idToken -> new FirebaseVerifiedToken("test-user", "user@example.com", "Example User", true);
		}
	}

}
