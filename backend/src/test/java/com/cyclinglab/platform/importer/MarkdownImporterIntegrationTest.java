package com.cyclinglab.platform.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cyclinglab.platform.auth.AuthService;
import com.cyclinglab.platform.auth.LoginRequest;
import com.cyclinglab.platform.auth.RegisterRequest;
import com.cyclinglab.platform.auth.TokenResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarkdownImporterIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;

    @Test
    void importsProfileAndTemplates(@TempDir Path tmp) throws Exception {
        Path profile = tmp.resolve("rider-profile.md");
        Files.writeString(profile, """
            ---
            title: "lxx"
            type: rider-profile
            height_cm: 176
            weight_kg: 70
            max_hr: 203
            ftp: 200
            ---
            # rider
            """);

        Path library = tmp.resolve("library");
        Files.createDirectories(library.resolve("endurance"));
        Files.createDirectories(library.resolve("intervals"));
        Files.writeString(library.resolve("endurance").resolve("z2.md"), """
            ---
            title: "Z2 基础"
            type: reusable-plan
            category: endurance
            intensity: Z2
            tags: [z2, outdoor]
            ---
            # Z2 基础
            ## 训练结构
            | 阶段 | 时间 | 强度 | 说明 |
            | --- | --- | --- | --- |
            | 热身 | 10 分钟 | Z1-Z2 | 渐进 |
            | 主训练 | 60 分钟 | Z2 | 平稳 |
            | 放松 | 5 分钟 | Z1 | 降心率 |
            """);
        Files.writeString(library.resolve("intervals").resolve("vo2.md"), """
            ---
            title: "VO2max 间歇"
            type: reusable-plan
            category: intervals
            intensity: VO2
            ---
            # VO2max
            ## 训练结构
            | 阶段 | 时间 | 强度 |
            | --- | --- | --- |
            | 热身 | 15 分钟 | Z2 |
            | 间歇 | 4 x 3 分钟 | Z5 |
            | 放松 | 10 分钟 | Z1 |
            """);
        Files.writeString(library.resolve("intervals").resolve("unknown.md"), """
            ---
            title: "Uncategorized Plan"
            type: reusable-plan
            ---
            # Whatever
            """);

        String token = registerAndLogin("importer-");

        mockMvc.perform(post("/api/v1/library/import-from-md")
                .header("Authorization", "Bearer " + token)
                .param("profilePath", profile.toString())
                .param("libraryPath", library.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.riderProfile.status").value("IMPORTED"))
            .andExpect(jsonPath("$.templates.length()").value(3));
    }

    private String registerAndLogin(String prefix) {
        String email = prefix + System.currentTimeMillis() + "@example.com";
        authService.register(new RegisterRequest(email, "Rider", "password123"));
        TokenResponse login = authService.login(new LoginRequest(email, "password123"));
        assertThat(login.accessToken()).isNotBlank();
        return login.accessToken();
    }
}