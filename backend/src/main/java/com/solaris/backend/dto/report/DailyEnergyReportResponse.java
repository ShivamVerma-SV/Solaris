package com.solaris.backend.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyEnergyReportResponse(
        LocalDate date,
        Long siteId,
        String siteName,
        BigDecimal productionKwh,
        BigDecimal consumptionKwh,
        BigDecimal gridImportKwh,
        BigDecimal gridExportKwh,
        long readingCount
) {
}
