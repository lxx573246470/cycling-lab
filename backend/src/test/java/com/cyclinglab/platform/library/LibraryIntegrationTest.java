package com.cyclinglab.platform.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cyclinglab.platform.auth.AuthService;
import com.cyclinglab.platform.auth.LoginRequest;
import com.cyclinglab.platform.auth.RegisterRequest;
import com.cyclinglab.platform.auth.TokenResponse;
import com.cyclinglab.platform.library.dto.WorkoutTemplateCreateRequest;
import com.cyclinglab.platform.library.dto.WorkoutTemplateDto;
import com.cyclinglab.platform.library.dto.WorkoutTemplatePutRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;

    private static final String VALID_STRUCTURE = """
        {"blocks":[
          {"type":"warmup","durationSec":600,"powerLow":0.45,"powerHigh":0.62},
          {"type":"steady","durationSec":1800,"power":0.65},
          {"type":"cooldown","durationSec":300,"powerLow":0.55,"powerHigh":0.40}
        ]}""";

    @Test
    void createAndGetTemplate() throws Exception {
        String token = registerAndLogin("lib-create-");
        WorkoutTemplateCreateRequest req = new WorkoutTemplateCreateRequest(
            "Z2 基础", "endurance", "Z2", "基础课", VALID_STRUCTURE, List.of("z2", "outdoor"));

        String body = objectMapper.writeValueAsString(req);

        MvcResult created = mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.currentVersion").value(1))
            .andExpect(jsonPath("$.name").value("Z2 基础"))
            .andReturn();
        WorkoutTemplateDto dto = objectMapper.readValue(
            created.getResponse().getContentAsByteArray(), WorkoutTemplateDto.class);
        UUID id = dto.id();

        mockMvc.perform(get("/api/v1/library/templates/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void structureWithExcessivePowerReturns422WithPointer() throws Exception {
        String token = registerAndLogin("lib-422-");
        // 1.6 > MAX_POWER_FACTOR(1.5) on block 0
        String structure = """
            {"blocks":[
              {"type":"steady","durationSec":600,"power":1.6}
            ]}""";
        WorkoutTemplateCreateRequest req = new WorkoutTemplateCreateRequest(
            "BadPower", "endurance", "Z2", "x", structure, List.of());

        mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("UNPROCESSABLE_STRUCTURE"))
            .andExpect(jsonPath("$.details[0].pointer").value("/blocks/0/power"));
    }

    @Test
    void putReplacesStructureAndBumpsVersion() throws Exception {
        String token = registerAndLogin("lib-put-");
        WorkoutTemplateCreateRequest create = new WorkoutTemplateCreateRequest(
            "Vo2max", "intervals", "VO2", "x", VALID_STRUCTURE, List.of());
        MvcResult r = mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID id = objectMapper.readValue(
            r.getResponse().getContentAsByteArray(), WorkoutTemplateDto.class).id();

        // PUT with changed structure
        String newStructure = """
            {"blocks":[
              {"type":"steady","durationSec":2400,"power":0.70}
            ]}""";
        WorkoutTemplatePutRequest put = new WorkoutTemplatePutRequest(
            "Vo2max", "intervals", "VO2", "x", newStructure, List.of(), "tightened");
        mockMvc.perform(put("/api/v1/library/templates/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(put)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentVersion").value(2));

        // versions endpoint should list 2 versions
        mockMvc.perform(get("/api/v1/library/templates/" + id + "/versions")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].version").value(2));
    }

    @Test
    void duplicateNameForSameUserReturns409() throws Exception {
        String token = registerAndLogin("lib-409-");
        WorkoutTemplateCreateRequest req = new WorkoutTemplateCreateRequest(
            "Dups", "recovery", "Z1", "x", VALID_STRUCTURE, List.of());
        mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_NAME"));
    }

    @Test
    void duplicateEndpointCreatesCopyWithDifferentName() throws Exception {
        String token = registerAndLogin("lib-dup-");
        WorkoutTemplateCreateRequest req = new WorkoutTemplateCreateRequest(
            "Source", "intervals", "VO2", "x", VALID_STRUCTURE, List.of());
        MvcResult r = mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID sourceId = objectMapper.readValue(
            r.getResponse().getContentAsByteArray(), WorkoutTemplateDto.class).id();

        mockMvc.perform(post("/api/v1/library/templates/" + sourceId + "/duplicate")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Source (副本)"));
    }

    @Test
    void anotherUserCannotSeeThisTemplate() throws Exception {
        String aliceToken = registerAndLogin("lib-alice-");
        String bobToken = registerAndLogin("lib-bob-");

        WorkoutTemplateCreateRequest req = new WorkoutTemplateCreateRequest(
            "AlicePrivate", "endurance", "Z2", "x", VALID_STRUCTURE, List.of());
        MvcResult r = mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID aliceId = objectMapper.readValue(
            r.getResponse().getContentAsByteArray(), WorkoutTemplateDto.class).id();

        // Bob hits Alice's id -> 404
        mockMvc.perform(get("/api/v1/library/templates/" + aliceId)
                .header("Authorization", "Bearer " + bobToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void archiveHidesFromList() throws Exception {
        String token = registerAndLogin("lib-archive-");
        WorkoutTemplateCreateRequest req = new WorkoutTemplateCreateRequest(
            "ToArchive", "endurance", "Z2", "x", VALID_STRUCTURE, List.of());
        MvcResult r = mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID id = objectMapper.readValue(
            r.getResponse().getContentAsByteArray(), WorkoutTemplateDto.class).id();

        // Archive (DELETE) - 204
        mockMvc.perform(delete("/api/v1/library/templates/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        // List without archived=true should not include it
        mockMvc.perform(get("/api/v1/library/templates?q=ToArchive")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));

        // But the detail still works
        mockMvc.perform(get("/api/v1/library/templates/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void categoriesEndpointReturnsSix() throws Exception {
        String token = registerAndLogin("lib-cats-");
        mockMvc.perform(get("/api/v1/library/categories")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(6));
    }

    private String registerAndLogin(String prefix) {
        String email = prefix + System.currentTimeMillis() + "@example.com";
        authService.register(new RegisterRequest(email, "Rider", "password123"));
        TokenResponse login = authService.login(new LoginRequest(email, "password123"));
        assertThat(login.accessToken()).isNotBlank();
        return login.accessToken();
    }
}
