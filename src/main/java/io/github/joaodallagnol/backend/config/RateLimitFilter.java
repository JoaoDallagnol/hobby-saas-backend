package io.github.joaodallagnol.backend.config;

import io.github.joaodallagnol.backend.auth.FirebaseAuthenticatedPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int RETENTION_MULTIPLIER = 3;

    private final RateLimitProperties properties;
    private final Clock clock;
    private final Map<String, TokenBucketState> buckets = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(RateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    RateLimitFilter(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }

        String path = request.getRequestURI();
        return path.startsWith("/actuator/health")
                || path.equals("/error")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Instant now = clock.instant();
        Duration refillInterval = Duration.ofMinutes(Math.max(1, properties.refillMinutes()));
        int capacity = Math.max(1, properties.capacity());
        int refillTokens = Math.max(1, properties.refillTokens());

        String key = resolveKey(request);
        TokenBucketState state = buckets.computeIfAbsent(key, ignored -> new TokenBucketState(capacity, now));
        boolean allowed = state.tryConsume(now, capacity, refillTokens, refillInterval);
        evictStaleBuckets(now, refillInterval);

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {"type":"about:blank","title":"Too Many Requests","status":429,"detail":"Rate limit exceeded. Try again later."}
                    """.trim());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof FirebaseAuthenticatedPrincipal principal) {
            return "user:" + principal.id();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            String clientIp = forwardedFor.split(",")[0].trim();
            if (StringUtils.hasText(clientIp)) {
                return "ip:" + clientIp;
            }
        }

        return "ip:" + Optional.ofNullable(request.getRemoteAddr()).orElse("unknown");
    }

    private void evictStaleBuckets(Instant now, Duration refillInterval) {
        long retentionMillis = refillInterval.multipliedBy(RETENTION_MULTIPLIER).toMillis();
        buckets.entrySet().removeIf(entry -> entry.getValue().isStale(now, retentionMillis));
    }

    private static final class TokenBucketState {

        private final AtomicInteger tokens;
        private volatile Instant lastRefillAt;

        private TokenBucketState(int capacity, Instant createdAt) {
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillAt = createdAt;
        }

        private synchronized boolean tryConsume(Instant now, int capacity, int refillTokens, Duration refillInterval) {
            refillIfNeeded(now, capacity, refillTokens, refillInterval);
            if (tokens.get() <= 0) {
                return false;
            }
            tokens.decrementAndGet();
            return true;
        }

        private void refillIfNeeded(Instant now, int capacity, int refillTokens, Duration refillInterval) {
            long elapsedMillis = Duration.between(lastRefillAt, now).toMillis();
            long intervalMillis = refillInterval.toMillis();
            if (elapsedMillis < intervalMillis) {
                return;
            }

            long intervals = elapsedMillis / intervalMillis;
            long replenished = intervals * (long) refillTokens;
            int updated = (int) Math.min(capacity, tokens.get() + replenished);
            tokens.set(updated);
            lastRefillAt = lastRefillAt.plusMillis(intervals * intervalMillis);
        }

        private boolean isStale(Instant now, long retentionMillis) {
            return Duration.between(lastRefillAt, now).toMillis() > retentionMillis;
        }
    }
}
