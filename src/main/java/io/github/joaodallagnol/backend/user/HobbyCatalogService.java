package io.github.joaodallagnol.backend.user;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HobbyCatalogService {

    private final HobbyRepository hobbyRepository;

    public HobbyCatalogService(HobbyRepository hobbyRepository) {
        this.hobbyRepository = hobbyRepository;
    }

    @Transactional(readOnly = true)
    public List<HobbyCatalogResponse> listHobbies() {
        return hobbyRepository.findAllByOrderByCategoryNameAscNameAsc().stream()
                .map(HobbyCatalogResponse::from)
                .toList();
    }
}
