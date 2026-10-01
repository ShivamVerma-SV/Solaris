package com.solaris.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "storage_readings",
        uniqueConstraints = @UniqueConstraint(name = "uk_storage_battery_timestamp", columnNames = {"battery_id", "recorded_at"}),
        indexes = {
                @Index(name = "idx_storage_site_time", columnList = "site_id,recorded_at"),
                @Index(name = "idx_storage_battery_time", columnList = "battery_id,recorded_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private SolarSite site;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "battery_id", nullable = false)
    private Battery battery;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal chargePercent;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal storedEnergyKwh;

    @Column(name = "recorded_at", nullable = false)
    private Instant timestamp;
}
