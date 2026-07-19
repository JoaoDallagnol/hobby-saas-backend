package io.github.joaodallagnol.backend.config;

import io.github.joaodallagnol.backend.auth.FirebaseAuthenticatedPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowRequestsWithinLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(
                new RateLimitProperties(true, 2, 2, 1),
                Clock.fixed(Instant.parse("2026-07-19T18:00:00Z"), ZoneOffset.UTC)
        );

        MockHttpServletResponse firstResponse = execute(filter, authenticatedRequest("user-1"));
        MockHttpServletResponse secondResponse = execute(filter, authenticatedRequest("user-1"));

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldRejectRequestWhenLimitIsExceeded() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(
                new RateLimitProperties(true, 1, 1, 1),
                Clock.fixed(Instant.parse("2026-07-19T18:00:00Z"), ZoneOffset.UTC)
        );

        MockHttpServletResponse firstResponse = execute(filter, authenticatedRequest("user-1"));
        MockHttpServletResponse secondResponse = execute(filter, authenticatedRequest("user-1"));

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getContentType()).isEqualTo("application/problem+json");
        assertThat(secondResponse.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    void shouldUseUserIdentityInsteadOfSharedIpForAuthenticatedRequests() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(
                new RateLimitProperties(true, 1, 1, 1),
                Clock.fixed(Instant.parse("2026-07-19T18:00:00Z"), ZoneOffset.UTC)
        );

        MockHttpServletResponse userOneResponse = execute(filter, authenticatedRequest("user-1"));
        MockHttpServletResponse userTwoResponse = execute(filter, authenticatedRequest("user-2"));

        assertThat(userOneResponse.getStatus()).isEqualTo(200);
        assertThat(userTwoResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldBypassHealthEndpoint() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(
                new RateLimitProperties(true, 1, 1, 1),
                Clock.fixed(Instant.parse("2026-07-19T18:00:00Z"), ZoneOffset.UTC)
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse execute(RateLimitFilter filter, MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletRequest authenticatedRequest(String userId) {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(
                userId,
                userId + "@example.com",
                "User " + userId,
                true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "token")
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sessions");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10");
        return request;
    }
}
