package com.solaris.backend.dto.storage;

import com.solaris.backend.entity.BatteryStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record BatteryResponse(
        Long id,
        String identifier,
        String name,
        Long siteId,
        String siteName,
        Long ownerId,
        BigDecimal capacityKwh,
        BatteryStatus status,
        BigDecimal currentChargePercent,
        BigDecimal currentStoredEnergyKwh,
        Instant lastUpdatedAt
) {
}
