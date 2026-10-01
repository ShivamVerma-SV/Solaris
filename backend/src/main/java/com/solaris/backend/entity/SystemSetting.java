package com.solaris.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {
    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal lowBatteryThreshold;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal highConsumptionThresholdKwh;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal lowProductionThresholdKwh;

    @Column(nullable = false)
    private boolean productionAlertsEnabled;

    @Column(nullable = false)
    private boolean consumptionAlertsEnabled;

    @Column(nullable = false)
    private boolean batteryAlertsEnabled;

    @Column(nullable = false)
    private boolean deviceOfflineAlertEnabled;

    @Column(nullable = false)
    private int deviceOfflineThresholdMinutes;

    @Column(nullable = false)
    private boolean emailNotificationsEnabled;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = Instant.now();
    }
}
