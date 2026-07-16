package io.github.joaodallagnol.backend.user;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import java.time.OffsetDateTime;
import org.springframework.dao.DataIntegrityViolationException;
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
        ProductUser productUser = new ProductUser(
                authenticatedUser.id(),
                authenticatedUser.email(),
                authenticatedUser.name(),
                authenticatedUser.emailVerified(),
                null,
                OffsetDateTime.now()
        );

        try {
            return productUserRepository.save(productUser);
        } catch (DataIntegrityViolationException ex) {
            return productUserRepository.findById(authenticatedUser.id())
                    .orElseThrow(() -> ex);
        }
    }
}
