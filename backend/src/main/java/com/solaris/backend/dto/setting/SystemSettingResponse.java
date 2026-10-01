package com.solaris.backend.dto.setting;

import java.math.BigDecimal;
import java.time.Instant;

public record SystemSettingResponse(
        BigDecimal lowBatteryThreshold,
        BigDecimal highConsumptionThresholdKwh,
        BigDecimal lowProductionThresholdKwh,
        boolean productionAlertsEnabled,
        boolean consumptionAlertsEnabled,
        boolean batteryAlertsEnabled,
        boolean deviceOfflineAlertEnabled,
        int deviceOfflineThresholdMinutes,
        boolean emailNotificationsEnabled,
        Instant updatedAt
) {
}
