package com.cyclinglab.platform.workout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cyclinglab.platform.auth.AuthService;
import com.cyclinglab.platform.auth.LoginRequest;
import com.cyclinglab.platform.auth.RegisterRequest;
import com.cyclinglab.platform.auth.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
class WorkoutFileIntegrationTest {

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
    void generateZwoFromInlineStructure() throws Exception {
        String token = registerAndLogin("zwo-inline-");
        ObjectNode body = objectMapper.createObjectNode()
            .put("name", "Z2 Base")
            .put("description", "Steady Z2 base ride")
            .put("structureJson", VALID_STRUCTURE)
            .set("tags", objectMapper.valueToTree(List.of("z2", "endurance")));

        MvcResult r = mockMvc.perform(post("/api/v1/workout-files")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Z2 Base"))
            .andExpect(jsonPath("$.format").value("ZWO"))
            .andExpect(jsonPath("$.sizeBytes").isNumber())
            .andExpect(jsonPath("$.xml").value(org.hamcrest.Matchers.containsString("<workout_file>")))
            .andExpect(jsonPath("$.xml").value(org.hamcrest.Matchers.containsString("<SteadyState Duration=\"1800\" Power=\"0.65\"/>")))
            .andExpect(jsonPath("$.xml").value(org.hamcrest.Matchers.containsString("<Warmup Duration=\"600\" PowerLow=\"0.45\" PowerHigh=\"0.62\"/>")))
            .andReturn();
        var json = objectMapper.readTree(r.getResponse().getContentAsByteArray());
        UUID id = UUID.fromString(json.get("id").asText());
        assertThat(json.get("sourceTemplateId").isNull()).isTrue();
    }

    @Test
    void generateZwoFromTemplateReferencesSource() throws Exception {
        String token = registerAndLogin("zwo-tpl-");
        // create a template
        ObjectNode tpl = objectMapper.createObjectNode()
            .put("name", "Sweet Spot")
            .put("category", "intervals")
            .put("intensity", "sweet-spot")
            .put("structureJson", VALID_STRUCTURE)
            .set("tags", objectMapper.valueToTree(List.of("sweet-spot")));
        MvcResult tplR = mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(tpl)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID tplId = UUID.fromString(objectMapper.readTree(
            tplR.getResponse().getContentAsByteArray()).get("id").asText());

        ObjectNode body = objectMapper.createObjectNode()
            .put("name", "Sweet Spot Generated")
            .put("sourceTemplateId", tplId.toString())
            .put("structureJson", VALID_STRUCTURE);
        mockMvc.perform(post("/api/v1/workout-files")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sourceTemplateId").value(tplId.toString()));
    }

    @Test
    void downloadReturnsXmlAttachment() throws Exception {
        String token = registerAndLogin("zwo-dl-");
        ObjectNode body = objectMapper.createObjectNode()
            .put("name", "My Workout")
            .put("structureJson", VALID_STRUCTURE);
        MvcResult r = mockMvc.perform(post("/api/v1/workout-files")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
            r.getResponse().getContentAsByteArray()).get("id").asText());

        mockMvc.perform(get("/api/v1/workout-files/" + id + "/download")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/xml"))
            .andExpect(header().string("Content-Disposition",
                org.hamcrest.Matchers.containsString("attachment")))
            .andExpect(header().string("Content-Disposition",
                org.hamcrest.Matchers.containsString("My+Workout.zwo")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<workout_file>")));
    }

    @Test
    void invalidStructureReturns422() throws Exception {
        String token = registerAndLogin("zwo-bad-");
        ObjectNode body = objectMapper.createObjectNode()
            .put("name", "Bad")
            .put("structureJson", "{\"blocks\":[{\"type\":\"steady\",\"durationSec\":60,\"power\":1.9}]}");
        mockMvc.perform(post("/api/v1/workout-files")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("UNPROCESSABLE_STRUCTURE"))
            .andExpect(jsonPath("$.details[0].pointer").value("/blocks/0/power"));
    }

    @Test
    void anotherUserCannotSeeThisWorkoutFile() throws Exception {
        String alice = registerAndLogin("zwo-iso-alice-");
        String bob = registerAndLogin("zwo-iso-bob-");

        ObjectNode body = objectMapper.createObjectNode()
            .put("name", "AliceOnly")
            .put("structureJson", VALID_STRUCTURE);
        MvcResult r = mockMvc.perform(post("/api/v1/workout-files")
                .header("Authorization", "Bearer " + alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID aliceId = UUID.fromString(objectMapper.readTree(
            r.getResponse().getContentAsByteArray()).get("id").asText());

        mockMvc.perform(get("/api/v1/workout-files/" + aliceId)
                .header("Authorization", "Bearer " + bob))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/workout-files/" + aliceId + "/download")
                .header("Authorization", "Bearer " + bob))
            .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsOwnedFilesInDescendingOrder() throws Exception {
        String token = registerAndLogin("zwo-list-");
        for (String name : new String[] { "First", "Second", "Third" }) {
            ObjectNode body = objectMapper.createObjectNode()
                .put("name", name)
                .put("structureJson", VALID_STRUCTURE);
            mockMvc.perform(post("/api/v1/workout-files")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated());
            Thread.sleep(5); // ensure distinct created_at
        }
        mockMvc.perform(get("/api/v1/workout-files")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].name").value("Third"))
            .andExpect(jsonPath("$.content[1].name").value("Second"))
            .andExpect(jsonPath("$.content[2].name").value("First"));
    }

    @Test
    void deleteRemovesWorkoutFile() throws Exception {
        String token = registerAndLogin("zwo-del-");
        ObjectNode body = objectMapper.createObjectNode()
            .put("name", "X")
            .put("structureJson", VALID_STRUCTURE);
        MvcResult r = mockMvc.perform(post("/api/v1/workout-files")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
            r.getResponse().getContentAsByteArray()).get("id").asText());

        mockMvc.perform(delete("/api/v1/workout-files/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/workout-files/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    private String registerAndLogin(String prefix) {
        String email = prefix + System.currentTimeMillis() + "@example.com";
        authService.register(new RegisterRequest(email, "Rider", "password123"));
        TokenResponse login = authService.login(new LoginRequest(email, "password123"));
        assertThat(login.accessToken()).isNotBlank();
        return login.accessToken();
    }
}