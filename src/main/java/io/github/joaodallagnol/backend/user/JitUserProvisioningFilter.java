package io.github.joaodallagnol.backend.user;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JitUserProvisioningFilter extends OncePerRequestFilter {

    private final AuthenticatedUserExtractor authenticatedUserExtractor;
    private final UserProvisioningService userProvisioningService;

    public JitUserProvisioningFilter(
            AuthenticatedUserExtractor authenticatedUserExtractor,
            UserProvisioningService userProvisioningService
    ) {
        this.authenticatedUserExtractor = authenticatedUserExtractor;
        this.userProvisioningService = userProvisioningService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                AuthenticatedUser authenticatedUser = authenticatedUserExtractor.extract(authentication);
                userProvisioningService.provisionIfMissing(authenticatedUser);
            } catch (IllegalArgumentException ex) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"invalid_token\",\"message\":\"" + ex.getMessage() + "\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
