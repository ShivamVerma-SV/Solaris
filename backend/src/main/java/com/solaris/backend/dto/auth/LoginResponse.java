package com.solaris.backend.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long accessTokenExpiresInSeconds;
    private long refreshTokenExpiresInSeconds;

    private Long userId;
    private String name;
    private String email;
    private String role;
}
