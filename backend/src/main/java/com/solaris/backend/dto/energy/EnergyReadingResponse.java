package com.solaris.backend.dto.energy;

import java.math.BigDecimal;
import java.time.Instant;

public record EnergyReadingResponse(
        Long id,
        Long siteId,
        String siteName,
        Long deviceId,
        String deviceIdentifier,
        BigDecimal productionKwh,
        BigDecimal consumptionKwh,
        BigDecimal gridImportKwh,
        BigDecimal gridExportKwh,
        Instant timestamp
) {
}
