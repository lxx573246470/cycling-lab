package com.cyclinglab.platform.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cyclinglab.platform.auth.AuthService;
import com.cyclinglab.platform.auth.LoginRequest;
import com.cyclinglab.platform.auth.RegisterRequest;
import com.cyclinglab.platform.auth.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;

    @Test
    void getReturnsEmptyWhenProfileMissing() throws Exception {
        String token = registerAndLogin("profile-empty-");
        mockMvc.perform(get("/api/v1/profile").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
    }

    @Test
    void putThenGetRoundTrips() throws Exception {
        String token = registerAndLogin("profile-upsert-");
        String body = """
            {
              "displayName": "lxx",
              "heightCm": 176,
              "weightKg": 70.0,
              "maxHr": 203,
              "restingHr": 49,
              "thresholdHr": 180,
              "ftp": 200,
              "cadenceLow": 80,
              "cadenceHigh": 95,
              "bikes": [],
              "goals": {"short_term": "test"},
              "preferences": {"weekly_days": 4}
            }
            """;
        mockMvc.perform(put("/api/v1/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("lxx"))
            .andExpect(jsonPath("$.ftp").value(200));

        mockMvc.perform(get("/api/v1/profile").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.maxHr").value(203));
    }

    @Test
    void patchUpdatesOnlyProvidedFields() throws Exception {
        String token = registerAndLogin("profile-patch-");
        String upsert = """
            {
              "displayName": "lxx",
              "heightCm": 176,
              "weightKg": 70.0,
              "maxHr": 203,
              "ftp": 200,
              "cadenceLow": 80,
              "cadenceHigh": 95
            }
            """;
        mockMvc.perform(put("/api/v1/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(upsert))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ftp\":210}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ftp").value(210))
            .andExpect(jsonPath("$.maxHr").value(203));
    }

    @Test
    void derivedZonesReturnsSevenHrAndPowerZones() throws Exception {
        String token = registerAndLogin("profile-zones-");
        String upsert = """
            {
              "displayName": "lxx",
              "heightCm": 176,
              "weightKg": 70.0,
              "maxHr": 200,
              "ftp": 200,
              "cadenceLow": 80,
              "cadenceHigh": 95
            }
            """;
        mockMvc.perform(put("/api/v1/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(upsert))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/profile/derived-zones")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hrZones.length()").value(7))
            .andExpect(jsonPath("$.powerZones.length()").value(7))
            .andExpect(jsonPath("$.ftp").value(200))
            .andExpect(jsonPath("$.maxHr").value(200));
    }

    @Test
    void validationFailsWhenFieldsOutOfRange() throws Exception {
        String token = registerAndLogin("profile-validation-");
        String body = """
            {
              "displayName": "lxx",
              "heightCm": 50,
              "weightKg": 70.0,
              "maxHr": 203,
              "ftp": 200,
              "cadenceLow": 80,
              "cadenceHigh": 95
            }
            """;
        mockMvc.perform(put("/api/v1/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String registerAndLogin(String prefix) {
        String email = prefix + System.currentTimeMillis() + "@example.com";
        authService.register(new RegisterRequest(email, "Rider", "password123"));
        TokenResponse login = authService.login(new LoginRequest(email, "password123"));
        assertThat(login.accessToken()).isNotBlank();
        return login.accessToken();
    }
}
