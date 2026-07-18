package io.github.joaodallagnol.backend.session;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hobbies/{hobbyId}/attribute-templates")
public class HobbyAttributeTemplateController {

    private final HobbyAttributeTemplateService hobbyAttributeTemplateService;

    public HobbyAttributeTemplateController(HobbyAttributeTemplateService hobbyAttributeTemplateService) {
        this.hobbyAttributeTemplateService = hobbyAttributeTemplateService;
    }

    @GetMapping
    public List<HobbyAttributeTemplateResponse> listTemplates(@PathVariable UUID hobbyId) {
        return hobbyAttributeTemplateService.listTemplates(hobbyId);
    }
}
