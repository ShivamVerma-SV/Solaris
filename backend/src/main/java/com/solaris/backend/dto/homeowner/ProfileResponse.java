package com.solaris.backend.dto.homeowner;

import java.time.Instant;

public record ProfileResponse(
        Long id,
        String name,
        String email,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {
}
