package com.cyclinglab.platform.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cyclinglab.platform.auth.AuthService;
import com.cyclinglab.platform.auth.LoginRequest;
import com.cyclinglab.platform.auth.RegisterRequest;
import com.cyclinglab.platform.auth.TokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TrainingFileIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;

    @Test
    void uploadParseListDetailSamplesDelete() throws Exception {
        String token = registerAndLogin("m3-");
        byte[] fit = loadFixture();
        assertThat(fit).isNotEmpty();

        MockMultipartFile mp = new MockMultipartFile(
            "file", "2026-05-21.fit", "application/octet-stream", fit
        );

        // 1) Upload
        MvcResult upload = mockMvc.perform(multipart("/api/v1/trainings/files")
                .file(mp)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("READY"))
            .andExpect(jsonPath("$.session").exists())
            .andExpect(jsonPath("$.session.durationSec").isNumber())
            .andExpect(jsonPath("$.session.avgHr").isNumber())
            .andExpect(jsonPath("$.session.normalizedPower").isNumber())
            .andExpect(jsonPath("$.isoYear").value(2026))
            .andExpect(jsonPath("$.isoWeek").value(21))
            .andReturn();
        JsonNode detail = objectMapper.readTree(upload.getResponse().getContentAsByteArray());
        UUID id = UUID.fromString(detail.get("id").asText());

        // 2) List
        mockMvc.perform(get("/api/v1/trainings/files?page=0&size=10")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(id.toString()))
            .andExpect(jsonPath("$.content[0].status").value("READY"));

        // 3) Detail
        mockMvc.perform(get("/api/v1/trainings/files/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.sha256").isString())
            .andExpect(jsonPath("$.session.bestRolling").isArray())
            .andExpect(jsonPath("$.session.tenMinSegments").isArray())
            .andExpect(jsonPath("$.session.hrZoneDistribution").isArray());

        // 4) Samples
        mockMvc.perform(get("/api/v1/trainings/files/" + id + "/samples?page=0&size=10")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").isNumber());

        // 5) Delete
        mockMvc.perform(delete("/api/v1/trainings/files/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/trainings/files/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void anotherUserCannotSeeThisTraining() throws Exception {
        String alice = registerAndLogin("m3-alice-");
        String bob = registerAndLogin("m3-bob-");
        byte[] fit = loadFixture();

        MockMultipartFile mp = new MockMultipartFile(
            "file", "ride.fit", "application/octet-stream", fit
        );
        MvcResult r = mockMvc.perform(multipart("/api/v1/trainings/files")
                .file(mp)
                .header("Authorization", "Bearer " + alice))
            .andExpect(status().isCreated())
            .andReturn();
        UUID aliceId = UUID.fromString(objectMapper.readTree(
            r.getResponse().getContentAsByteArray()).get("id").asText());

        mockMvc.perform(get("/api/v1/trainings/files/" + aliceId)
                .header("Authorization", "Bearer " + bob))
            .andExpect(status().isNotFound());
    }

    @Test
    void uploadRejectsNonFitFile() throws Exception {
        String token = registerAndLogin("m3-bad-");
        MockMultipartFile mp = new MockMultipartFile(
            "file", "note.txt", "text/plain", "hello".getBytes()
        );
        mockMvc.perform(multipart("/api/v1/trainings/files")
                .file(mp)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());
    }

    private byte[] loadFixture() throws IOException {
        Path candidate = Paths.get("..", "training", "2026", "week-21", "fit", "2026-05-21.fit");
        if (!Files.isRegularFile(candidate)) {
            candidate = Paths.get("training", "2026", "week-21", "fit", "2026-05-21.fit");
        }
        if (!Files.isRegularFile(candidate)) {
            throw new IOException("FIT fixture not found in repo (../training/2026/week-21/fit/2026-05-21.fit)");
        }
        return Files.readAllBytes(candidate);
    }

    private String registerAndLogin(String prefix) {
        String email = prefix + System.currentTimeMillis() + "@example.com";
        authService.register(new RegisterRequest(email, "Rider", "password123"));
        TokenResponse login = authService.login(new LoginRequest(email, "password123"));
        assertThat(login.accessToken()).isNotBlank();
        return login.accessToken();
    }
}
