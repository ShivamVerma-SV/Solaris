package com.solaris.backend.dto.device;

import com.solaris.backend.entity.DeviceStatus;
import com.solaris.backend.entity.DeviceType;

import java.time.Instant;

public record DeviceResponse(
        Long id,
        String identifier,
        String name,
        DeviceType type,
        DeviceStatus status,
        Long siteId,
        String siteName,
        Long ownerId,
        String manufacturer,
        String model,
        String firmwareVersion,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt
) {
}
