package com.solaris.backend.security;

import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.repository.SolarSiteRepository;
import com.solaris.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

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
    private SolarSiteRepository siteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User admin;
    private User homeowner;

    @BeforeEach
    void setUp() {
        siteRepository.deleteAll();
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
    void malformedBearerHeaderReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Malformed Authorization header"));
    }

    @Test
    void homeownerCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(jwtService.generateAccessToken(homeowner))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCannotAccessHomeownerProfile() throws Exception {
        mockMvc.perform(get("/api/homeowner/profile")
                        .header("Authorization", bearer(jwtService.generateAccessToken(admin))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void homeownerCannotReadAnotherHomeownersSiteByChangingId() throws Exception {
        User otherOwner = userRepository.save(user("other@example.com", UserRole.HOMEOWNER));
        SolarSite site = siteRepository.save(SolarSite.builder()
                .owner(otherOwner)
                .code("OTHER-HOME")
                .name("Other home")
                .capacityKw(new BigDecimal("4.500"))
                .active(true)
                .build());

        mockMvc.perform(get("/api/homeowner/sites/{id}", site.getId())
                        .header("Authorization", bearer(jwtService.generateAccessToken(homeowner))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Solar site not found"));
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

    @Test
    void missingRequiredRegistrationFieldsReturnFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").isArray())
                .andExpect(jsonPath("$.fieldErrors.email").isArray())
                .andExpect(jsonPath("$.fieldErrors.password").isArray());
    }

    @Test
    void invalidDeviceEnumReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/devices")
                        .header("Authorization", bearer(jwtService.generateAccessToken(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "INV-INVALID",
                                  "name": "Invalid inverter",
                                  "type": "NOT_A_DEVICE_TYPE",
                                  "status": "ONLINE",
                                  "siteId": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void missingUserIdReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", Long.MAX_VALUE)
                        .header("Authorization", bearer(jwtService.generateAccessToken(admin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void energyEndpointsReturnEmptyDomainResults() throws Exception {
        String accessToken = jwtService.generateAccessToken(homeowner);
        mockMvc.perform(get("/api/homeowner/energy/readings")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/homeowner/energy/summary")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productionKwh").value(0))
                .andExpect(jsonPath("$.readingCount").value(0));
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
