package com.solaris.backend.dto.storage;

import com.solaris.backend.entity.BatteryStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class BatteryRequest {
    @NotBlank
    @Size(max = 100)
    private String identifier;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotNull
    @Positive
    private Long siteId;

    @NotNull
    @Positive
    private BigDecimal capacityKwh;

    @NotNull
    private BatteryStatus status;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal currentChargePercent;

    @NotNull
    @PositiveOrZero
    private BigDecimal currentStoredEnergyKwh;

    @NotNull
    private Instant lastUpdatedAt;
}
