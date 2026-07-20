package io.github.joaodallagnol.backend.user;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hobbies")
public class HobbyCatalogController {

    private final HobbyCatalogService hobbyCatalogService;

    public HobbyCatalogController(HobbyCatalogService hobbyCatalogService) {
        this.hobbyCatalogService = hobbyCatalogService;
    }

    @GetMapping
    public List<HobbyCatalogResponse> listHobbies() {
        return hobbyCatalogService.listHobbies();
    }
}
