package com.solaris.backend.dto.alert;

import com.solaris.backend.entity.AlertSeverity;
import com.solaris.backend.entity.AlertType;

import java.time.Instant;

public record AlertResponse(
        Long id,
        AlertType type,
        AlertSeverity severity,
        String message,
        boolean read,
        Instant createdAt,
        Long userId,
        Long siteId,
        Long deviceId,
        Long batteryId
) {
}
