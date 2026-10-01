package com.solaris.backend.security;

import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {
    private JwtService jwtService;
    private UserRepository userRepository;
    private JwtAuthenticationFilter filter;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-only-secret-key-that-is-at-least-thirty-two-bytes-long",
                Duration.ofMinutes(15),
                Duration.ofDays(7)
        );
        userRepository = mock(UserRepository.class);
        filter = new JwtAuthenticationFilter(
                jwtService,
                userRepository,
                new SecurityErrorWriter(new ObjectMapper())
        );
        user = User.builder()
                .id(3L)
                .email("owner@example.com")
                .name("Owner")
                .password("unused")
                .role(UserRole.HOMEOWNER)
                .enabled(true)
                .build();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingHeaderContinuesWithoutAuthentication() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var continued = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void malformedHeaderReturnsJsonUnauthorized() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/homeowner/profile");
        request.addHeader("Authorization", "Token abc");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("Malformed Authorization header");
    }

    @Test
    void refreshTokenCannotAuthenticateRequest() throws Exception {
        var request = bearerRequest(jwtService.generateRefreshToken(user));
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Only access tokens");
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        var request = bearerRequest("not-a-jwt");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("invalid or expired");
    }

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        JwtService expiredTokenService = new JwtService(
                "test-only-secret-key-that-is-at-least-thirty-two-bytes-long",
                Duration.ofSeconds(-1),
                Duration.ofDays(7)
        );
        var request = bearerRequest(expiredTokenService.generateAccessToken(user));
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("invalid or expired");
    }

    @Test
    void validAccessTokenBuildsAuthenticatedPrincipal() throws Exception {
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        var request = bearerRequest(jwtService.generateAccessToken(user));
        var response = new MockHttpServletResponse();
        var continued = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> continued.set(true));

        assertThat(continued).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(new AuthenticatedUser(3L, "owner@example.com", UserRole.HOMEOWNER));
    }

    private MockHttpServletRequest bearerRequest(String token) {
        var request = new MockHttpServletRequest("GET", "/api/homeowner/profile");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
