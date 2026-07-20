package io.github.joaodallagnol.backend.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HobbyCatalogControllerTest {

    @Test
    void shouldExposeHobbyCatalogContract() throws Exception {
        UUID hobbyId = UUID.fromString("1f1f49ea-6b5d-4c2e-9ce7-3e621f081001");
        HobbyCatalogService service = new HobbyCatalogService(null) {
            @Override
            public List<HobbyCatalogResponse> listHobbies() {
                return List.of(new HobbyCatalogResponse(hobbyId, "Running", "Sports & Movement", "figure.run"));
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HobbyCatalogController(service)).build();

        mockMvc.perform(get("/api/hobbies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(hobbyId.toString()))
                .andExpect(jsonPath("$[0].name").value("Running"))
                .andExpect(jsonPath("$[0].categoryName").value("Sports & Movement"))
                .andExpect(jsonPath("$[0].icon").value("figure.run"));
    }
}
