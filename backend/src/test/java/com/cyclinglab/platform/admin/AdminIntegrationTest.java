package com.cyclinglab.platform.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cyclinglab.platform.auth.AuthService;
import com.cyclinglab.platform.auth.LoginRequest;
import com.cyclinglab.platform.auth.RegisterRequest;
import com.cyclinglab.platform.auth.TokenResponse;
import com.cyclinglab.platform.user.UserEntity;
import com.cyclinglab.platform.user.UserRepository;
import com.cyclinglab.platform.user.UserRole;
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
class AdminIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepo;

    @Test
    void nonAdminCannotAccessAdminEndpoint() throws Exception {
        String token = registerAndLogin("m7-user-");
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAndFilterUsers() throws Exception {
        // Register two regular users, then promote one of them to admin.
        String adminToken = registerAndPromoteToAdmin("m7-admin-");
        registerAndLogin("m7-bystander-1-");
        registerAndLogin("m7-bystander-2-");

        mockMvc.perform(get("/api/v1/admin/users?size=100")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").isNumber());

        mockMvc.perform(get("/api/v1/admin/users?role=ADMIN&size=100")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].role").value("ADMIN"));
    }

    @Test
    void adminCanPatchRoleAndStatus() throws Exception {
        String adminToken = registerAndPromoteToAdmin("m7-promote-");
        String userToken = registerAndLogin("m7-target-");
        UUID userId = currentUserId(userToken);

        ObjectNode body = objectMapper.createObjectNode()
            .put("role", "ADMIN")
            .put("status", "DISABLED");
        mockMvc.perform(patch("/api/v1/admin/users/" + userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.status").value("DISABLED"));

        // The disabled user can no longer authenticate.
        String email = userRepo.findById(userId).orElseThrow().getEmail();
        try {
            authService.login(new LoginRequest(email, "password123"));
            assertThat(false).as("disabled user should not be able to log in").isTrue();
        } catch (Exception expected) {
            // good
        }
    }

    @Test
    void adminCanDeleteUser() throws Exception {
        String adminToken = registerAndPromoteToAdmin("m7-deleter-");
        String userToken = registerAndLogin("m7-victim-");
        UUID userId = currentUserId(userToken);

        mockMvc.perform(delete("/api/v1/admin/users/" + userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/users/" + userId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void adminCannotDeleteSelf() throws Exception {
        String adminToken = registerAndPromoteToAdmin("m7-self-");
        UUID selfId = currentUserId(adminToken);

        mockMvc.perform(delete("/api/v1/admin/users/" + selfId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getMissingUserReturns404() throws Exception {
        String adminToken = registerAndPromoteToAdmin("m7-missing-");
        UUID missing = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/admin/users/" + missing)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    // ---- helpers ----------------------------------------------------------

    private String registerAndLogin(String prefix) {
        String email = prefix + System.currentTimeMillis() + "@example.com";
        authService.register(new RegisterRequest(email, "Rider", "password123"));
        TokenResponse login = authService.login(new LoginRequest(email, "password123"));
        return login.accessToken();
    }

    private String registerAndPromoteToAdmin(String prefix) {
        String email = prefix + System.currentTimeMillis() + "@example.com";
        authService.register(new RegisterRequest(email, "Admin", "password123"));
        UserEntity u = userRepo.findByEmail(email).orElseThrow();
        u.setRole(UserRole.ADMIN);
        userRepo.save(u);
        TokenResponse login = authService.login(new LoginRequest(email, "password123"));
        return login.accessToken();
    }

    private UUID currentUserId(String token) {
        // Decode the JWT subject (we trust the token here because the test
        // framework just minted it).
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        try {
            return UUID.fromString(objectMapper.readTree(payload).get("sub").asText());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot decode sub from test token", e);
        }
    }
}