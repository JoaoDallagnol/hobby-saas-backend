package io.github.joaodallagnol.backend.user;

import io.github.joaodallagnol.backend.gamification.UserBadge;
import io.github.joaodallagnol.backend.gamification.UserBadgeRepository;
import io.github.joaodallagnol.backend.gamification.UserFeaturedBadge;
import io.github.joaodallagnol.backend.gamification.UserFeaturedBadgeRepository;
import io.github.joaodallagnol.backend.session.ResourceNotFoundException;
import io.github.joaodallagnol.backend.subscription.EntitlementService;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProfileCustomizationService {

    private static final Set<String> THEMES = Set.of("default", "midnight", "forest", "sunset", "ocean");

    private final ProductUserRepository userRepository;
    private final UserBadgeRepository badgeRepository;
    private final UserFeaturedBadgeRepository featuredRepository;
    private final EntitlementService entitlementService;

    public ProfileCustomizationService(ProductUserRepository userRepository, UserBadgeRepository badgeRepository,
                                       UserFeaturedBadgeRepository featuredRepository,
                                       EntitlementService entitlementService) {
        this.userRepository = userRepository;
        this.badgeRepository = badgeRepository;
        this.featuredRepository = featuredRepository;
        this.entitlementService = entitlementService;
    }

    public ProfileCustomizationResponse current() {
        String userId = entitlementService.currentUserId();
        return response(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found.")));
    }

    @Transactional
    public ProfileCustomizationResponse update(ProfileCustomizationRequest request) {
        String userId = entitlementService.currentUserId();
        entitlementService.requirePlus(userId);
        String theme = request.theme().trim().toLowerCase(java.util.Locale.ROOT);
        if (!THEMES.contains(theme)) {
            throw new IllegalArgumentException("Unsupported profile theme.");
        }
        List<UUID> badgeIds = request.featuredBadgeIds() == null ? List.of() : request.featuredBadgeIds();
        if (new HashSet<>(badgeIds).size() != badgeIds.size()) {
            throw new IllegalArgumentException("Featured badges cannot contain duplicates.");
        }
        List<UserBadge> badges = badgeRepository.findAllById(badgeIds);
        if (badges.size() != badgeIds.size() || badges.stream().anyMatch(badge -> !badge.getUserId().equals(userId))) {
            throw new IllegalArgumentException("One or more badges do not belong to the user.");
        }
        ProductUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.updateProfileTheme(theme);
        featuredRepository.deleteAllByUserId(userId);
        for (int index = 0; index < badgeIds.size(); index++) {
            UUID badgeId = badgeIds.get(index);
            UserBadge badge = badges.stream().filter(item -> item.getId().equals(badgeId)).findFirst().orElseThrow();
            featuredRepository.save(new UserFeaturedBadge(userId, index + 1, badge));
        }
        return response(user);
    }

    public ProfileCustomizationResponse responseForUser(ProductUser user) {
        return response(user);
    }

    private ProfileCustomizationResponse response(ProductUser user) {
        List<FeaturedBadgeResponse> featured = featuredRepository.findAllByUserIdOrderByPositionAsc(user.getId())
                .stream().map(item -> new FeaturedBadgeResponse(item.getBadge().getId(), item.getBadge().getBadgeKey(),
                        item.getBadge().getHobby() == null ? null : item.getBadge().getHobby().getId(),
                        item.getPosition())).toList();
        return new ProfileCustomizationResponse(user.getProfileTheme(), entitlementService.hasPlus(user.getId()), featured);
    }
}
