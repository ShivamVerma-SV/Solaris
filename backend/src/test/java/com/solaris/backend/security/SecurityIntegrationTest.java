package com.solaris.backend.security;

import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User admin;
    private User homeowner;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        admin = userRepository.save(user("admin@example.com", UserRole.ADMIN));
        homeowner = userRepository.save(user("owner@example.com", UserRole.HOMEOWNER));
    }

    @Test
    void missingAuthorizationHeaderReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/admin/users"));
    }

    @Test
    void homeownerCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(jwtService.generateAccessToken(homeowner))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanAccessPaginatedUserEndpointWithoutPasswordExposure() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(jwtService.generateAccessToken(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void refreshTokenCannotAuthenticateProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(jwtService.generateRefreshToken(admin))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Only access tokens may authenticate API requests"));
    }

    @Test
    void invalidPaginationIsReportedAsValidationError() throws Exception {
        mockMvc.perform(get("/api/admin/users?page=-1&size=101")
                        .header("Authorization", bearer(jwtService.generateAccessToken(admin))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void malformedJsonReturnsConsistentError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    private User user(String email, UserRole role) {
        return User.builder()
                .name(role.name())
                .email(email)
                .password(passwordEncoder.encode("password-123"))
                .role(role)
                .enabled(true)
                .build();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
