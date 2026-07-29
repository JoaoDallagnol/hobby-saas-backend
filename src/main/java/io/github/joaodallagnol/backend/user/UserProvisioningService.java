package io.github.joaodallagnol.backend.user;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProvisioningService {

    private final ProductUserRepository productUserRepository;

    public UserProvisioningService(ProductUserRepository productUserRepository) {
        this.productUserRepository = productUserRepository;
    }

    @Transactional
    public ProductUser provisionIfMissing(AuthenticatedUser authenticatedUser) {
        return productUserRepository.findById(authenticatedUser.id())
                .orElseGet(() -> createUser(authenticatedUser));
    }

    private ProductUser createUser(AuthenticatedUser authenticatedUser) {
        productUserRepository.insertIfMissing(
                authenticatedUser.id(),
                authenticatedUser.email(),
                authenticatedUser.name(),
                authenticatedUser.emailVerified(),
                OffsetDateTime.now()
        );

        return productUserRepository.findById(authenticatedUser.id())
                .orElseThrow(() -> new IllegalStateException("Authenticated user could not be provisioned."));
    }
}
