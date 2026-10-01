package com.solaris.backend.service;

import com.solaris.backend.dto.auth.LoginRequest;
import com.solaris.backend.dto.auth.LoginResponse;
import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.exception.UnauthorizedException;
import com.solaris.backend.repository.UserRepository;
import com.solaris.backend.security.JwtService;
import com.solaris.backend.security.RefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private static final String SECRET = "test-only-secret-key-that-is-at-least-thirty-two-bytes-long";

    private UserRepository userRepository;
    private InMemoryRefreshTokenStore tokenStore;
    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenStore = new InMemoryRefreshTokenStore();
        var passwordEncoder = new BCryptPasswordEncoder(4);
        var jwtService = new JwtService(SECRET, Duration.ofMinutes(15), Duration.ofDays(7));
        authService = new AuthService(userRepository, passwordEncoder, jwtService, tokenStore);
        user = User.builder()
                .id(7L)
                .name("Solar Owner")
                .email("owner@example.com")
                .password(passwordEncoder.encode("correct-password"))
                .role(UserRole.HOMEOWNER)
                .enabled(true)
                .build();
    }

    @Test
    void loginIssuesAndStoresRefreshToken() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(loginRequest());

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getAccessTokenExpiresInSeconds()).isEqualTo(900);
        assertThat(tokenStore.entries).hasSize(1);
        assertThat(tokenStore.entries.values().iterator().next()).doesNotContain(response.getRefreshToken());
    }

    @Test
    void refreshRotatesTokenAndRejectsReplay() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        String original = authService.login(loginRequest()).getRefreshToken();

        LoginResponse rotated = authService.refresh(original);

        assertThat(rotated.getRefreshToken()).isNotEqualTo(original);
        assertThat(tokenStore.entries).hasSize(1);
        assertThatThrownBy(() -> authService.refresh(original))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void accessTokenCannotBeUsedForRefresh() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        String accessToken = authService.login(loginRequest()).getAccessToken();

        assertThatThrownBy(() -> authService.refresh(accessToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void refreshRejectsDeletedOrDisabledUserAfterConsumingToken() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        String deletedUserToken = authService.login(loginRequest()).getRefreshToken();
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(deletedUserToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("no longer exists");

        String disabledUserToken = authService.login(loginRequest()).getRefreshToken();
        user.setEnabled(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.refresh(disabledUserToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void logoutRevokesRefreshToken() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        String refreshToken = authService.login(loginRequest()).getRefreshToken();

        authService.logout(refreshToken);

        assertThat(tokenStore.entries).isEmpty();
        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(UnauthorizedException.class);
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("OWNER@example.com");
        request.setPassword("correct-password");
        return request;
    }

    private static final class InMemoryRefreshTokenStore implements RefreshTokenStore {
        private final Map<String, String> entries = new HashMap<>();

        @Override
        public void store(String tokenId, Long userId, String tokenHash, Duration ttl) {
            entries.put(tokenId, userId + ":" + tokenHash);
        }

        @Override
        public boolean consume(String tokenId, Long userId, String tokenHash) {
            return entries.remove(tokenId, userId + ":" + tokenHash);
        }
    }
}
