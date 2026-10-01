package com.solaris.backend.dto.site;

import java.math.BigDecimal;
import java.time.Instant;

public record SiteResponse(
        Long id,
        Long ownerId,
        String ownerName,
        String code,
        String name,
        String address,
        BigDecimal capacityKw,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
