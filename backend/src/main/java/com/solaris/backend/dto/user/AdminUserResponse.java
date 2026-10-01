package com.solaris.backend.dto.user;

import com.solaris.backend.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AdminUserResponse {

    private Long id;
    private String name;
    private String email;
    private UserRole role;
    private boolean enabled;
    private String phone;
    private Instant createdAt;
    private Instant updatedAt;
}
