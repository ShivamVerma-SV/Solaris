package com.solaris.backend.service;

import com.solaris.backend.dto.auth.LoginRequest;
import com.solaris.backend.dto.auth.LoginResponse;
import com.solaris.backend.dto.auth.RegisterRequest;
import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.exception.ConflictException;
import com.solaris.backend.exception.UnauthorizedException;
import com.solaris.backend.repository.UserRepository;
import com.solaris.backend.security.JwtService;
import com.solaris.backend.security.RefreshTokenStore;
import com.solaris.backend.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.HOMEOWNER)
                .build();

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return issueTokenPair(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(String refreshToken) {
        RefreshTokenDetails token = parseRefreshToken(refreshToken);
        if (!refreshTokenStore.consume(token.tokenId(), token.userId(), hash(refreshToken))) {
            throw new UnauthorizedException("Refresh token is revoked or has already been used");
        }

        User user = userRepository.findById(token.userId())
                .orElseThrow(() -> new UnauthorizedException("Refresh token user no longer exists"));
        if (!user.isEnabled() || !user.getEmail().equalsIgnoreCase(token.email())) {
            throw new UnauthorizedException("Refresh token user is not allowed to authenticate");
        }

        return issueTokenPair(user);
    }

    public void logout(String refreshToken) {
        RefreshTokenDetails token = parseRefreshToken(refreshToken);
        if (!refreshTokenStore.consume(token.tokenId(), token.userId(), hash(refreshToken))) {
            throw new UnauthorizedException("Refresh token is revoked or has already been used");
        }
    }

    private LoginResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        Claims claims = jwtService.extractClaims(refreshToken);
        Duration remainingTtl = Duration.between(Instant.now(), claims.getExpiration().toInstant());
        if (remainingTtl.isNegative() || remainingTtl.isZero()) {
            throw new IllegalStateException("Generated refresh token has no usable lifetime");
        }
        refreshTokenStore.store(claims.getId(), user.getId(), hash(refreshToken), remainingTtl);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresInSeconds(jwtService.getAccessTokenExpiresInSeconds())
                .refreshTokenExpiresInSeconds(jwtService.getRefreshTokenExpiresInSeconds())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private RefreshTokenDetails parseRefreshToken(String refreshToken) {
        try {
            Claims claims = jwtService.extractClaims(refreshToken);
            String type = claims.get("type", String.class);
            Number userId = claims.get("userId", Number.class);
            if (!TokenType.REFRESH.name().equals(type)
                    || claims.getId() == null
                    || claims.getSubject() == null
                    || userId == null) {
                throw new UnauthorizedException("Invalid refresh token");
            }
            return new RefreshTokenDetails(claims.getId(), userId.longValue(), claims.getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record RefreshTokenDetails(String tokenId, Long userId, String email) {
    }
}
