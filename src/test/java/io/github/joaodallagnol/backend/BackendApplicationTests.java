package io.github.joaodallagnol.backend;

import io.github.joaodallagnol.backend.user.ProductUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
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
		ProductUserRepository productUserRepository() {
			return org.mockito.Mockito.mock(ProductUserRepository.class);
		}
	}

}
