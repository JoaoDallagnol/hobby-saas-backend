package io.github.joaodallagnol.backend.user;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.github.joaodallagnol.backend.config.CacheNames.HOBBY_CATALOG;

@Service
public class HobbyCatalogService {

    private final HobbyRepository hobbyRepository;

    public HobbyCatalogService(HobbyRepository hobbyRepository) {
        this.hobbyRepository = hobbyRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = HOBBY_CATALOG, sync = true)
    public List<HobbyCatalogResponse> listHobbies() {
        return hobbyRepository.findAllByOrderByCategoryNameAscNameAsc().stream()
                .map(HobbyCatalogResponse::from)
                .toList();
    }
}
