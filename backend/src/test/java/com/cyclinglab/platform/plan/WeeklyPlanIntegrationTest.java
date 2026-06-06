package com.cyclinglab.platform.plan;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
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
class WeeklyPlanIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;

    private static final String VALID_STRUCTURE = """
        {"blocks":[
          {"type":"warmup","durationSec":600,"powerLow":0.45,"powerHigh":0.62},
          {"type":"steady","durationSec":1800,"power":0.65},
          {"type":"cooldown","durationSec":300,"powerLow":0.55,"powerHigh":0.40}
        ]}""";

    // Use a fixed ISO week so the test is deterministic and never drifts into
    // "this year only has 52 weeks" territory.
    private static final int ISO_YEAR = 2026;
    private static final int ISO_WEEK = 24; // 2026 W24 starts Mon 2026-06-08

    @Test
    void createWeeklyPlanAutoFillsSevenDays() throws Exception {
        String token = registerAndLogin("plan-create-");

        ObjectNode body = objectMapper.createObjectNode()
            .put("isoYear", ISO_YEAR)
            .put("isoWeek", ISO_WEEK)
            .put("title", "Build week")
            .put("goalMd", "Add 1h Z2 + 2x threshold");

        MvcResult r = mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.isoYear").value(ISO_YEAR))
            .andExpect(jsonPath("$.isoWeek").value(ISO_WEEK))
            .andExpect(jsonPath("$.days.length()").value(7))
            .andExpect(jsonPath("$.days[0].weekday").value(1))
            .andExpect(jsonPath("$.days[6].weekday").value(7))
            .andExpect(jsonPath("$.progress.total").value(7))
            .andExpect(jsonPath("$.progress.planned").value(7))
            .andReturn();

        var detail = objectMapper.readTree(r.getResponse().getContentAsByteArray());
        LocalDate firstDate = LocalDate.parse(detail.get("days").get(0).get("date").asText());
        LocalDate lastDate = LocalDate.parse(detail.get("days").get(6).get("date").asText());
        assertThat(firstDate.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(lastDate.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        // Same ISO year+week as the request
        int[] first = IsoWeek.of(firstDate);
        assertThat(first[0]).isEqualTo(ISO_YEAR);
        assertThat(first[1]).isEqualTo(ISO_WEEK);
    }

    @Test
    void createDuplicateWeekReturns409WithConflictWith() throws Exception {
        String token = registerAndLogin("plan-dup-");
        ObjectNode body = objectMapper.createObjectNode()
            .put("isoYear", ISO_YEAR).put("isoWeek", 25)
            .put("title", "First");

        MvcResult r = mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID firstId = UUID.fromString(objectMapper.readTree(
            r.getResponse().getContentAsByteArray()).get("id").asText());

        mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("WEEK_PLAN_EXISTS"))
            .andExpect(jsonPath("$.details[0].conflictWith").value(firstId.toString()));
    }

    @Test
    void listAndGetReturnOwnedPlan() throws Exception {
        String token = registerAndLogin("plan-list-");
        // create two plans in different weeks
        for (int w : new int[] { 22, 23 }) {
            mockMvc.perform(post("/api/v1/plans/weeks")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(
                        objectMapper.createObjectNode()
                            .put("isoYear", ISO_YEAR).put("isoWeek", w)
                            .put("title", "W" + w))))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/plans/weeks?page=0&size=10")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].isoWeek").value(23))
            .andExpect(jsonPath("$.content[1].isoWeek").value(22));
    }

    @Test
    void updateTitleAndGoalMd() throws Exception {
        String token = registerAndLogin("plan-update-");
        ObjectNode body = objectMapper.createObjectNode()
            .put("isoYear", ISO_YEAR).put("isoWeek", 21)
            .put("title", "Old");
        MvcResult r = mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isCreated())
            .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
            r.getResponse().getContentAsByteArray()).get("id").asText());

        ObjectNode patch = objectMapper.createObjectNode()
            .put("title", "New")
            .put("goalMd", "Pace the long ride");

        mockMvc.perform(put("/api/v1/plans/weeks/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(patch)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("New"))
            .andExpect(jsonPath("$.goalMd").value("Pace the long ride"));
    }

    @Test
    void updateDayAssignsTemplateAndChangesStatus() throws Exception {
        String token = registerAndLogin("plan-day-");
        // create a template
        MvcResult tpl = mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WorkoutTemplateCreateRequest(
                    "Z2 Base", "endurance", "Z2", "x", VALID_STRUCTURE, List.of()
                ))))
            .andExpect(status().isCreated())
            .andReturn();
        UUID tplId = objectMapper.readValue(
            tpl.getResponse().getContentAsByteArray(), WorkoutTemplateDto.class).id();

        // create a weekly plan
        MvcResult plan = mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    objectMapper.createObjectNode()
                        .put("isoYear", ISO_YEAR).put("isoWeek", 20)
                        .put("title", "Plan"))))
            .andExpect(status().isCreated())
            .andReturn();
        var planJson = objectMapper.readTree(plan.getResponse().getContentAsByteArray());
        UUID dayId = UUID.fromString(planJson.get("days").get(0).get("id").asText());

        // update the day: assign template + status=DONE + notes
        ObjectNode patch = objectMapper.createObjectNode()
            .put("targetText", "60min Z2")
            .put("templateId", tplId.toString())
            .put("notesMd", "Felt strong")
            .put("status", "DONE");

        mockMvc.perform(put("/api/v1/plans/weeks/" + planJson.get("id").asText() + "/days/" + dayId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(patch)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.templateId").value(tplId.toString()))
            .andExpect(jsonPath("$.templateName").value("Z2 Base"))
            .andExpect(jsonPath("$.status").value("DONE"))
            .andExpect(jsonPath("$.targetText").value("60min Z2"));

        // and progress reflects it
        mockMvc.perform(get("/api/v1/plans/weeks/" + planJson.get("id").asText())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.progress.done").value(1))
            .andExpect(jsonPath("$.progress.planned").value(6));
    }

    @Test
    void updateDayClearingTemplateViaFlag() throws Exception {
        String token = registerAndLogin("plan-clear-");
        MvcResult tpl = mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WorkoutTemplateCreateRequest(
                    "Z2 Base", "endurance", "Z2", "x", VALID_STRUCTURE, List.of()
                ))))
            .andExpect(status().isCreated())
            .andReturn();
        UUID tplId = objectMapper.readValue(
            tpl.getResponse().getContentAsByteArray(), WorkoutTemplateDto.class).id();

        MvcResult plan = mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    objectMapper.createObjectNode()
                        .put("isoYear", ISO_YEAR).put("isoWeek", 19).put("title", "Plan"))))
            .andExpect(status().isCreated())
            .andReturn();
        var planJson = objectMapper.readTree(plan.getResponse().getContentAsByteArray());
        UUID dayId = UUID.fromString(planJson.get("days").get(0).get("id").asText());

        // set then clear
        mockMvc.perform(put("/api/v1/plans/weeks/" + planJson.get("id").asText() + "/days/" + dayId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    objectMapper.createObjectNode().put("templateId", tplId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.templateId").value(tplId.toString()));

        // clear by sending templateIdPresent=true
        mockMvc.perform(put("/api/v1/plans/weeks/" + planJson.get("id").asText() + "/days/" + dayId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    objectMapper.createObjectNode().put("templateIdPresent", true))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.templateId").doesNotExist())
            .andExpect(jsonPath("$.templateName").doesNotExist());
    }

    @Test
    void updateDayRejectsTemplateOwnedByAnotherUser() throws Exception {
        String alice = registerAndLogin("plan-alice-");
        String bob = registerAndLogin("plan-bob-");

        // Alice owns a template
        MvcResult tpl = mockMvc.perform(post("/api/v1/library/templates")
                .header("Authorization", "Bearer " + alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new WorkoutTemplateCreateRequest(
                    "AliceTpl", "endurance", "Z2", "x", VALID_STRUCTURE, List.of()
                ))))
            .andExpect(status().isCreated())
            .andReturn();
        UUID aliceTpl = objectMapper.readValue(
            tpl.getResponse().getContentAsByteArray(), WorkoutTemplateDto.class).id();

        // Bob has his own plan and tries to assign Alice's template
        MvcResult plan = mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + bob)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    objectMapper.createObjectNode()
                        .put("isoYear", ISO_YEAR).put("isoWeek", 18).put("title", "Bob"))))
            .andExpect(status().isCreated())
            .andReturn();
        var planJson = objectMapper.readTree(plan.getResponse().getContentAsByteArray());
        UUID dayId = UUID.fromString(planJson.get("days").get(0).get("id").asText());

        mockMvc.perform(put("/api/v1/plans/weeks/" + planJson.get("id").asText() + "/days/" + dayId)
                .header("Authorization", "Bearer " + bob)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    objectMapper.createObjectNode().put("templateId", aliceTpl.toString()))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void anotherUserCannotSeeThisPlan() throws Exception {
        String alice = registerAndLogin("plan-iso-alice-");
        String bob = registerAndLogin("plan-iso-bob-");

        MvcResult plan = mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    objectMapper.createObjectNode()
                        .put("isoYear", ISO_YEAR).put("isoWeek", 17).put("title", "X"))))
            .andExpect(status().isCreated())
            .andReturn();
        UUID alicePlanId = UUID.fromString(objectMapper.readTree(
            plan.getResponse().getContentAsByteArray()).get("id").asText());

        mockMvc.perform(get("/api/v1/plans/weeks/" + alicePlanId)
                .header("Authorization", "Bearer " + bob))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/plans/weeks/" + alicePlanId)
                .header("Authorization", "Bearer " + bob))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesPlanAndDays() throws Exception {
        String token = registerAndLogin("plan-del-");
        MvcResult plan = mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    objectMapper.createObjectNode()
                        .put("isoYear", ISO_YEAR).put("isoWeek", 16).put("title", "x"))))
            .andExpect(status().isCreated())
            .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
            plan.getResponse().getContentAsByteArray()).get("id").asText());

        mockMvc.perform(delete("/api/v1/plans/weeks/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/plans/weeks/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void invalidIsoWeekInYearReturns400() throws Exception {
        String token = registerAndLogin("plan-bad-");
        // 2025 has 52 weeks; 53 is invalid
        mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    objectMapper.createObjectNode()
                        .put("isoYear", 2025).put("isoWeek", 53).put("title", "x"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void updateUnknownDayReturns404() throws Exception {
        String token = registerAndLogin("plan-404-");
        MvcResult plan = mockMvc.perform(post("/api/v1/plans/weeks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                    objectMapper.createObjectNode()
                        .put("isoYear", ISO_YEAR).put("isoWeek", 15).put("title", "x"))))
            .andExpect(status().isCreated())
            .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
            plan.getResponse().getContentAsByteArray()).get("id").asText());

        mockMvc.perform(put("/api/v1/plans/weeks/" + id + "/days/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void listPaginatesAndStaysWithinBounds() throws Exception {
        String token = registerAndLogin("plan-paging-");
        for (int w : new int[] { 1, 2, 3, 4, 5 }) {
            mockMvc.perform(post("/api/v1/plans/weeks")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(
                        objectMapper.createObjectNode()
                            .put("isoYear", ISO_YEAR).put("isoWeek", w).put("title", "W" + w))))
                .andExpect(status().isCreated());
        }
        mockMvc.perform(get("/api/v1/plans/weeks?page=0&size=2")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.content.length()").value(2));
    }

    private String registerAndLogin(String prefix) {
        String email = prefix + System.currentTimeMillis() + "@example.com";
        authService.register(new RegisterRequest(email, "Rider", "password123"));
        TokenResponse login = authService.login(new LoginRequest(email, "password123"));
        assertThat(login.accessToken()).isNotBlank();
        return login.accessToken();
    }

    // keep the imports honest for refactors
    @SuppressWarnings("unused")
    private static WeekFields __isoFields = WeekFields.ISO;
}