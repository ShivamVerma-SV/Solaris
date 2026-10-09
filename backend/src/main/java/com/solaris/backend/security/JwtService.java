package com.solaris.backend.security;

import com.solaris.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.boot.convert.DurationUnit;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl:15m}") @DurationUnit(ChronoUnit.SECONDS) Duration accessTokenTtl,
            @Value("${jwt.refresh-token-ttl:7d}") @DurationUnit(ChronoUnit.SECONDS) Duration refreshTokenTtl
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .claim("type", TokenType.ACCESS.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenTtl.toMillis()))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("type", TokenType.REFRESH.name())
                // The unique ID is also the Redis key, allowing each refresh token to be rotated independently.
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenTtl.toMillis()))
                .signWith(secretKey)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpiresInSeconds() {
        return accessTokenTtl.toSeconds();
    }

    public long getRefreshTokenExpiresInSeconds() {
        return refreshTokenTtl.toSeconds();
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }
}
