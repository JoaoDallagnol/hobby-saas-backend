package io.github.joaodallagnol.backend.user;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserProfileService {

    private final AuthenticatedUserExtractor authenticatedUserExtractor;
    private final ProductUserRepository productUserRepository;

    public CurrentUserProfileService(
            AuthenticatedUserExtractor authenticatedUserExtractor,
            ProductUserRepository productUserRepository
    ) {
        this.authenticatedUserExtractor = authenticatedUserExtractor;
        this.productUserRepository = productUserRepository;
    }

    public CurrentUserProfileResponse getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthenticatedUser authenticatedUser = authenticatedUserExtractor.extract(authentication);

        ProductUser productUser = productUserRepository.findById(authenticatedUser.id())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not provisioned."));

        return CurrentUserProfileResponse.from(productUser);
    }
}
