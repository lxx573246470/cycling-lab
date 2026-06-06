package com.cyclinglab.platform.review;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
class ReviewIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;

    @Test
    void createListGetUpdateDeleteWeeklyReview() throws Exception {
        String token = registerAndLogin("m4-week-");

        ObjectNode body = objectMapper.createObjectNode()
            .put("scope", "WEEK")
            .put("isoYear", 2026)
            .put("isoWeek", 22)
            .put("title", "Week 22 - Sweet spot intro")
            .put("contentMd", "## Highlights\n\n- Solid Z2 volume\n- 2x12 sweet spot");

        MvcResult r = mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.scope").value("WEEK"))
            .andExpect(jsonPath("$.isoYear").value(2026))
            .andExpect(jsonPath("$.isoWeek").value(22))
            .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
            r.getResponse().getContentAsByteArray()).get("id").asText());

        mockMvc.perform(get("/api/v1/reviews")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(id.toString()));

        mockMvc.perform(get("/api/v1/reviews/by-week?isoYear=2026&isoWeek=22")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Week 22 - Sweet spot intro"));

        mockMvc.perform(get("/api/v1/reviews/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contentMd").value(org.hamcrest.Matchers.containsString("Highlights")));

        ObjectNode put = objectMapper.createObjectNode()
            .put("isoYear", 2026)
            .put("isoWeek", 22)
            .put("title", "Week 22 - Updated title")
            .put("contentMd", "## Updated content");
        mockMvc.perform(put("/api/v1/reviews/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(put)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Week 22 - Updated title"));

        mockMvc.perform(delete("/api/v1/reviews/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/reviews/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void duplicateWeeklyReturnsConflict() throws Exception {
        String token = registerAndLogin("m4-dup-");
        ObjectNode body = objectMapper.createObjectNode()
            .put("scope", "WEEK")
            .put("isoYear", 2026)
            .put("isoWeek", 30)
            .put("title", "W30")
            .put("contentMd", "ok");
        mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("REVIEW_EXISTS"));
    }

    @Test
    void anotherUserCannotSeeThisReview() throws Exception {
        String alice = registerAndLogin("m4-alice-");
        String bob = registerAndLogin("m4-bob-");
        ObjectNode body = objectMapper.createObjectNode()
            .put("scope", "WEEK")
            .put("isoYear", 2026)
            .put("isoWeek", 25)
            .put("title", "Alice only")
            .put("contentMd", "");
        MvcResult r = mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", "Bearer " + alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID aliceId = UUID.fromString(objectMapper.readTree(
            r.getResponse().getContentAsByteArray()).get("id").asText());

        mockMvc.perform(get("/api/v1/reviews/" + aliceId)
                .header("Authorization", "Bearer " + bob))
            .andExpect(status().isNotFound());
    }

    @Test
    void phaseReviewRequiresScopeId() throws Exception {
        String token = registerAndLogin("m4-phase-");
        ObjectNode body = objectMapper.createObjectNode()
            .put("scope", "PHASE")
            .put("title", "Build phase")
            .put("contentMd", "12 weeks");
        mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidIsoWeekReturns400() throws Exception {
        String token = registerAndLogin("m4-badweek-");
        ObjectNode body = objectMapper.createObjectNode()
            .put("scope", "WEEK")
            .put("isoYear", 2026)
            .put("isoWeek", 60)
            .put("title", "Invalid")
            .put("contentMd", "");
        mockMvc.perform(post("/api/v1/reviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isBadRequest());
    }

    private String registerAndLogin(String prefix) {
        String email = prefix + System.currentTimeMillis() + "@example.com";
        authService.register(new RegisterRequest(email, "Rider", "password123"));
        TokenResponse login = authService.login(new LoginRequest(email, "password123"));
        assertThat(login.accessToken()).isNotBlank();
        return login.accessToken();
    }
}