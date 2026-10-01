package com.solaris.backend.dto.setting;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SystemSettingRequest {
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal lowBatteryThreshold;

    @NotNull
    @DecimalMin("0.000")
    private BigDecimal highConsumptionThresholdKwh;

    @NotNull
    @DecimalMin("0.000")
    private BigDecimal lowProductionThresholdKwh;

    private boolean productionAlertsEnabled;
    private boolean consumptionAlertsEnabled;
    private boolean batteryAlertsEnabled;
    private boolean deviceOfflineAlertEnabled;

    @Min(1)
    @Max(10080)
    private int deviceOfflineThresholdMinutes;

    private boolean emailNotificationsEnabled;
}
