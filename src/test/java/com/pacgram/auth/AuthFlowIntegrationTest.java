package com.pacgram.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrationLoginByUsernameAndAuthenticatedMeWorkTogether() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"PacGram User","username":"pacgram_user",
                                 "email":"user@example.com","password":"Str0ngPassw0rd"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("pacgram_user"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"PACGRAM_USER","password":"Str0ngPassw0rd"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginBody.path("data").path("token").asText();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@example.com"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"USER@EXAMPLE.COM","password":"Str0ngPassw0rd"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    void registrationReturnsStandardFieldErrorsForInvalidInput() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","username":"not valid","email":"bad","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andReturn();

        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(error.path("timestamp").asText()).isNotBlank();
    }

    @Test
    void protectedProfileRequiresJwtAndUsesStandardErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void swaggerApiDescriptionIsPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PacGram API"));
    }

    @Test
    void provisionedAdministratorCanSignInSeparatelyFromUsers() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"pacadmin-test","password":"TestOnlyAdminPassword123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("pacadmin-test"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void reportStoresEvidenceAndAutomaticallyBlocksReportedAccount() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String reporterToken = registerAndLogin("reporter_" + suffix, "reporter_" + suffix + "@example.com");
        String reportedUsername = "reported_" + suffix;
        String reportedToken = registerAndLogin(reportedUsername, reportedUsername + "@example.com");
        long reportedId = reportedUserId(reportedToken);
        MockMultipartFile evidence = new MockMultipartFile(
                "evidence", "proof.png", MediaType.IMAGE_PNG_VALUE,
                Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jS4kAAAAASUVORK5CYII="));

        MvcResult reportResult = mockMvc.perform(multipart("/api/reports/with-evidence")
                        .file(evidence)
                        .param("reportedUserId", String.valueOf(reportedId))
                        .param("reason", "HARASSMENT")
                        .param("details", "Repeated unwanted messages were sent.")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();
        long reportId = objectMapper.readTree(reportResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult adminLogin = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"pacadmin-test","password":"TestOnlyAdminPassword123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString())
                .path("data").path("token").asText();
        mockMvc.perform(get("/api/admin/users/{userId}", reportedId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account.username").value(reportedUsername))
                .andExpect(jsonPath("$.data.reportsReceivedCount").value(1));
        mockMvc.perform(get("/api/admin/users/{userId}/reports", reportedId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reporterUsername").value("reporter_" + suffix));
        mockMvc.perform(get("/api/admin/users/{userId}/posts", reportedId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get("/api/admin/users/{userId}/blocks", reportedId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        MvcResult reportPage = mockMvc.perform(get("/api/admin/reports")
                        .param("page", "0")
                        .param("size", "100")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode rows = objectMapper.readTree(reportPage.getResponse().getContentAsString())
                .path("data").path("content");
        JsonNode report = java.util.stream.StreamSupport.stream(rows.spliterator(), false)
                .filter(row -> row.path("id").asLong() == reportId)
                .findFirst().orElseThrow();
        long evidenceId = report.path("evidence").get(0).path("id").asLong();

        mockMvc.perform(get("/api/users/{username}", reportedUsername)
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isNotFound());
        MvcResult blockedAccounts = mockMvc.perform(get("/api/users/me/blocked")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(blockedAccounts.getResponse().getContentAsString())
                .path("data").path("content").toString()).contains(reportedUsername);

        mockMvc.perform(get("/api/reports/{reportId}/evidence/{evidenceId}", reportId, evidenceId)
                        .header("Authorization", "Bearer " + reportedToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reports/{reportId}/evidence/{evidenceId}", reportId, evidenceId)
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.IMAGE_PNG));
        mockMvc.perform(delete("/api/users/{id}/block", reportedId)
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/{username}", reportedUsername)
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/reports/{reportId}/evidence/{evidenceId}", reportId, evidenceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.IMAGE_PNG));
    }

    private String registerAndLogin(String username, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", "Report Test User",
                                "username", username,
                                "email", email,
                                "password", "Str0ngPassw0rd"))))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", username, "password", "Str0ngPassw0rd"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private long reportedUserId(String token) throws Exception {
        MvcResult user = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(user.getResponse().getContentAsString())
                .path("data").path("userId").asLong();
    }
}
