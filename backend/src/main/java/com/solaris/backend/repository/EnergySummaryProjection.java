package com.solaris.backend.repository;

import java.math.BigDecimal;

public interface EnergySummaryProjection {
    BigDecimal getProductionKwh();

    BigDecimal getConsumptionKwh();

    BigDecimal getGridImportKwh();

    BigDecimal getGridExportKwh();

    long getReadingCount();
}
