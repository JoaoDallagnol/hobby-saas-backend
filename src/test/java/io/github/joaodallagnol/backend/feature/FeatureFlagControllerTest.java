package io.github.joaodallagnol.backend.feature;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FeatureFlagControllerTest {

    @Test
    void shouldExposeFeatureFlagContract() throws Exception {
        FeatureFlagProperties properties = new FeatureFlagProperties();
        properties.setPhotoUploads(true);
        properties.setSessionLocation(false);
        properties.setPhotoProcessing(false);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new FeatureFlagController(new FeatureFlagService(properties))
        ).build();

        mockMvc.perform(get("/api/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUploads").value(true))
                .andExpect(jsonPath("$.sessionLocation").value(false))
                .andExpect(jsonPath("$.photoProcessing").value(false));
    }
}
