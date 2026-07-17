package io.github.joaodallagnol.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseTokenVerifier firebaseTokenVerifier;

    public FirebaseAuthenticationFilter(FirebaseTokenVerifier firebaseTokenVerifier) {
        this.firebaseTokenVerifier = firebaseTokenVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authorizationHeader.substring(7).trim();

        try {
            FirebaseVerifiedToken verifiedToken = firebaseTokenVerifier.verify(idToken);
            FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(
                    verifiedToken.userId(),
                    verifiedToken.email(),
                    verifiedToken.name(),
                    verifiedToken.emailVerified()
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    idToken,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token", ex.getMessage());
        } catch (IllegalStateException ex) {
            writeJsonError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "auth_unavailable", ex.getMessage());
        }
    }

    private void writeJsonError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
