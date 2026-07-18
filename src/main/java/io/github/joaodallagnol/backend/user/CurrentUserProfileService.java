package io.github.joaodallagnol.backend.user;

import io.github.joaodallagnol.backend.auth.AuthenticatedUser;
import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserProfileService {

    private final AuthenticatedUserExtractor authenticatedUserExtractor;
    private final ProductUserRepository productUserRepository;
    private final UserHobbyRepository userHobbyRepository;
    private final HobbyRepository hobbyRepository;

    public CurrentUserProfileService(
            AuthenticatedUserExtractor authenticatedUserExtractor,
            ProductUserRepository productUserRepository,
            UserHobbyRepository userHobbyRepository,
            HobbyRepository hobbyRepository
    ) {
        this.authenticatedUserExtractor = authenticatedUserExtractor;
        this.productUserRepository = productUserRepository;
        this.userHobbyRepository = userHobbyRepository;
        this.hobbyRepository = hobbyRepository;
    }

    public CurrentUserProfileResponse getCurrentUserProfile() {
        ProductUser productUser = getCurrentUser();

        return CurrentUserProfileResponse.from(productUser);
    }

    @Transactional
    public CurrentUserProfileResponse updateCurrentUserProfile(CurrentUserProfileUpdateRequest request) {
        ProductUser productUser = getCurrentUser();
        productUser.updateProfile(request.name().trim(), request.bio());
        return CurrentUserProfileResponse.from(productUser);
    }

    public List<UserHobbyResponse> getCurrentUserHobbies() {
        return userHobbyRepository.findAllByIdUserIdOrderByHobbyNameAsc(getAuthenticatedUser().id()).stream()
                .map(UserHobbyResponse::from)
                .toList();
    }

    @Transactional
    public UserHobbyResponse addCurrentUserHobby(AddUserHobbyRequest request) {
        AuthenticatedUser authenticatedUser = getAuthenticatedUser();
        ProductUser productUser = getCurrentUser();
        UUID hobbyId = request.hobbyId();

        if (userHobbyRepository.existsByIdUserIdAndIdHobbyId(authenticatedUser.id(), hobbyId)) {
            throw new IllegalArgumentException("Hobby is already linked to the user profile.");
        }

        Hobby hobby = hobbyRepository.findById(hobbyId)
                .orElseThrow(() -> new IllegalArgumentException("Hobby not found."));

        UserHobby userHobby = new UserHobby(productUser, hobby, request.experienceLevel());
        return UserHobbyResponse.from(userHobbyRepository.save(userHobby));
    }

    @Transactional
    public UserHobbyResponse updateCurrentUserHobby(UUID hobbyId, UpdateUserHobbyRequest request) {
        UserHobby userHobby = userHobbyRepository.findByIdUserIdAndIdHobbyId(getAuthenticatedUser().id(), hobbyId)
                .orElseThrow(() -> new IllegalArgumentException("User hobby not found."));

        userHobby.updateExperienceLevel(request.experienceLevel());
        return UserHobbyResponse.from(userHobby);
    }

    @Transactional
    public void removeCurrentUserHobby(UUID hobbyId) {
        UserHobby userHobby = userHobbyRepository.findByIdUserIdAndIdHobbyId(getAuthenticatedUser().id(), hobbyId)
                .orElseThrow(() -> new IllegalArgumentException("User hobby not found."));
        userHobbyRepository.delete(userHobby);
    }

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authenticatedUserExtractor.extract(authentication);
    }

    private ProductUser getCurrentUser() {
        AuthenticatedUser authenticatedUser = getAuthenticatedUser();
        return productUserRepository.findById(authenticatedUser.id())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not provisioned."));
    }
}
