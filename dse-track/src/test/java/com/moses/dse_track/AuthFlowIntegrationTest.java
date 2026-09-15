package com.moses.dse_track;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Exercises the whole stack together against a real (in-memory H2) schema:
// register -> receive a JWT -> use it to reach a protected endpoint -> log in
// again -> confirm an unauthenticated request is rejected. Unit tests above
// mock everything away; this is the one that proves it's all actually wired
// together correctly.
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Not autowired — Spring Boot 4.1 doesn't register a Jackson ObjectMapper
    // as a bean by default (Spring MVC builds its own internally for JSON
    // message conversion), so this test just builds its own for request bodies.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerLoginAndAccessAProtectedEndpoint() throws Exception {
        String email = "integration-" + System.nanoTime() + "@example.com";

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Integration Tester",
                                "email", email,
                                "password", "password123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("token").asText();

        // The freshly issued token can reach a protected endpoint
        mockMvc.perform(get("/portfolio").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdings").isArray())
                .andExpect(jsonPath("$.holdings").isEmpty());

        // Registering twice with the same email is rejected, not silently allowed
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Duplicate",
                                "email", email,
                                "password", "password123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already registered"));

        // Logging in with the same credentials issues a fresh, independently valid token
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // The wrong password is rejected with a generic message (no user enumeration)
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointRejectsRequestsWithNoToken() throws Exception {
        mockMvc.perform(get("/portfolio"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void protectedEndpointRejectsAGarbageToken() throws Exception {
        mockMvc.perform(get("/portfolio").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicStockListDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk());
    }
}
