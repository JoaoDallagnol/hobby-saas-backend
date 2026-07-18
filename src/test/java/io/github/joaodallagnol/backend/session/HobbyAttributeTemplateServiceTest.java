package io.github.joaodallagnol.backend.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.joaodallagnol.backend.auth.AuthenticatedUserExtractor;
import io.github.joaodallagnol.backend.auth.FirebaseAuthenticatedPrincipal;
import io.github.joaodallagnol.backend.user.Hobby;
import io.github.joaodallagnol.backend.user.HobbyCategory;
import io.github.joaodallagnol.backend.user.HobbyRepository;
import io.github.joaodallagnol.backend.user.UserHobbyRepository;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class HobbyAttributeTemplateServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldListTemplatesForUserHobbyOrderedByDisplayOrder() {
        UUID hobbyId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        Hobby hobby = ReflectionFactory.createHobby(hobbyId, "Running", "Sports & Movement");
        HobbyRepository hobbyRepository = (HobbyRepository) Proxy.newProxyInstance(
                HobbyRepository.class.getClassLoader(),
                new Class<?>[]{HobbyRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(hobby);
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "TestHobbyRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        UserHobbyRepository userHobbyRepository = (UserHobbyRepository) Proxy.newProxyInstance(
                UserHobbyRepository.class.getClassLoader(),
                new Class<?>[]{UserHobbyRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsByIdUserIdAndIdHobbyId" -> true;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "TestUserHobbyRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        List<HobbyAttributeTemplate> storage = new ArrayList<>();
        storage.add(ReflectionFactory.createTemplate(UUID.randomUUID(), hobby, "surface", "Surface", "text", null, 2));
        storage.add(ReflectionFactory.createTemplate(UUID.randomUUID(), hobby, "distance_km", "Distance", "number", "km", 1));
        HobbyAttributeTemplateRepository templateRepository = (HobbyAttributeTemplateRepository) Proxy.newProxyInstance(
                HobbyAttributeTemplateRepository.class.getClassLoader(),
                new Class<?>[]{HobbyAttributeTemplateRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAllByHobbyIdOrderByDisplayOrderAsc" -> storage.stream()
                            .filter(item -> item.getHobby().getId().equals(args[0]))
                            .sorted(Comparator.comparing(HobbyAttributeTemplate::getDisplayOrder))
                            .toList();
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "TestTemplateRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        authenticate("firebase-user-1", "user@example.com", "User");
        HobbyAttributeTemplateService service = new HobbyAttributeTemplateService(
                new AuthenticatedUserExtractor(),
                hobbyRepository,
                userHobbyRepository,
                templateRepository
        );

        List<HobbyAttributeTemplateResponse> response = service.listTemplates(hobbyId);

        assertThat(response).extracting(HobbyAttributeTemplateResponse::key)
                .containsExactly("distance_km", "surface");
    }

    private void authenticate(String userId, String email, String name) {
        FirebaseAuthenticatedPrincipal principal = new FirebaseAuthenticatedPrincipal(userId, email, name, true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "token"));
    }

    private static final class ReflectionFactory {
        static Hobby createHobby(UUID id, String name, String categoryName) {
            try {
                var categoryConstructor = HobbyCategory.class.getDeclaredConstructor();
                categoryConstructor.setAccessible(true);
                HobbyCategory category = categoryConstructor.newInstance();
                setField(HobbyCategory.class, category, "id", UUID.randomUUID());
                setField(HobbyCategory.class, category, "name", categoryName);

                var hobbyConstructor = Hobby.class.getDeclaredConstructor();
                hobbyConstructor.setAccessible(true);
                Hobby hobby = hobbyConstructor.newInstance();
                setField(Hobby.class, hobby, "id", id);
                setField(Hobby.class, hobby, "name", name);
                setField(Hobby.class, hobby, "icon", "icon");
                setField(Hobby.class, hobby, "category", category);
                return hobby;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        static HobbyAttributeTemplate createTemplate(UUID id, Hobby hobby, String key, String label, String type, String unit, int displayOrder) {
            try {
                var constructor = HobbyAttributeTemplate.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                HobbyAttributeTemplate template = constructor.newInstance();
                setField(HobbyAttributeTemplate.class, template, "id", id);
                setField(HobbyAttributeTemplate.class, template, "hobby", hobby);
                setField(HobbyAttributeTemplate.class, template, "key", key);
                setField(HobbyAttributeTemplate.class, template, "label", label);
                setField(HobbyAttributeTemplate.class, template, "type", type);
                setField(HobbyAttributeTemplate.class, template, "unit", unit);
                setField(HobbyAttributeTemplate.class, template, "displayOrder", displayOrder);
                return template;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private static void setField(Class<?> type, Object target, String fieldName, Object value) throws Exception {
            var field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
    }
}
