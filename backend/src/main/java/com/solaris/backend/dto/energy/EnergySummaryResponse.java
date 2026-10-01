package com.solaris.backend.dto.energy;

import java.math.BigDecimal;
import java.time.Instant;

public record EnergySummaryResponse(
        Long siteId,
        Instant from,
        Instant to,
        BigDecimal productionKwh,
        BigDecimal consumptionKwh,
        BigDecimal gridImportKwh,
        BigDecimal gridExportKwh,
        long readingCount
) {
}
