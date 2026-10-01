package com.solaris.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "energy_readings",
        uniqueConstraints = @UniqueConstraint(name = "uk_energy_device_timestamp", columnNames = {"device_id", "recorded_at"}),
        indexes = {
                @Index(name = "idx_energy_site_time", columnList = "site_id,recorded_at"),
                @Index(name = "idx_energy_device_time", columnList = "device_id,recorded_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnergyReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private SolarSite site;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal productionKwh;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal consumptionKwh;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal gridImportKwh;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal gridExportKwh;

    @Column(name = "recorded_at", nullable = false)
    private Instant timestamp;
}
