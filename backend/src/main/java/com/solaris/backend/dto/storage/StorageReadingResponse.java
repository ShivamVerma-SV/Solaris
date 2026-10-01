package com.solaris.backend.dto.storage;

import java.math.BigDecimal;
import java.time.Instant;

public record StorageReadingResponse(
        Long id,
        Long siteId,
        String siteName,
        Long batteryId,
        String batteryIdentifier,
        BigDecimal chargePercent,
        BigDecimal storedEnergyKwh,
        Instant timestamp
) {
}
