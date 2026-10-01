package com.solaris.backend.security;

import com.solaris.backend.entity.UserRole;

public record AuthenticatedUser(Long id, String email, UserRole role) {
}
