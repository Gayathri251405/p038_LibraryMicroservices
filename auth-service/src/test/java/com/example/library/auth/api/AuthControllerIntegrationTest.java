package com.example.library.auth.api;

import com.example.library.auth.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrationHashesPasswordAndIssuesStudentToken() throws Exception {
        String username = "reader-" + UUID.randomUUID();
        String password = "reader-password-123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.Credentials(username, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken", startsWith("eyJ")));

        var savedUser = users.findByUsername(username).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals(password, savedUser.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(savedUser.getPasswordHash().startsWith("$2"));
    }

    @Test
    void loginAcceptsValidCredentialsAndRejectsInvalidCredentials() throws Exception {
        String username = "login-" + UUID.randomUUID();
        String password = "reader-password-123";
        var credentials = new AuthController.Credentials(username, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.accessToken", startsWith("eyJ")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.Credentials(username, "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationDoesNotAllowClientsToChooseElevatedRoles() throws Exception {
        String username = "role-check-" + UUID.randomUUID();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username
                                + "\",\"password\":\"reader-password-123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", not("ADMIN")));
    }

    @Test
    void administratorsCanManagePatronAndLibrarianAccounts() throws Exception {
        var adminToken = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.Credentials("test-admin", "test-admin-password"))))
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(adminToken).get("accessToken").asText();
        String username = "managed-" + UUID.randomUUID();

        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.ManagedAccountRequest(username, "reader-password-123", "FACULTY"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("FACULTY"));

        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.ManagedAccountRequest(username + "-staff",
                                        "librarian-password-123", "LIBRARIAN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("LIBRARIAN"));

        var usersResponse = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/users")
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(usersResponse.contains(username));
        org.junit.jupiter.api.Assertions.assertTrue(usersResponse.contains("\"role\":\"FACULTY\""));
        org.junit.jupiter.api.Assertions.assertFalse(usersResponse.contains("passwordHash"));
    }

    @Test
    void userManagementRejectsPatronsAndClientSuppliedAdminAccounts() throws Exception {
        String username = "patron-" + UUID.randomUUID();
        var credentials = new AuthController.Credentials(username, "reader-password-123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(login).get("accessToken").asText();

        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.ManagedAccountRequest("forbidden-" + UUID.randomUUID(),
                                        "reader-password-123", "ADMIN"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.ManagedAccountRequest("untrusted-" + UUID.randomUUID(),
                                        "reader-password-123", "ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void configuredAdminAndLibrarianAccountsAreAvailable() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.Credentials("test-admin", "test-admin-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthController.Credentials("test-librarian", "test-librarian-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LIBRARIAN"));
    }
}
