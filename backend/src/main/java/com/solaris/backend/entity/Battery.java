package com.solaris.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "batteries", indexes = {
        @Index(name = "idx_batteries_site", columnList = "site_id"),
        @Index(name = "idx_batteries_identifier", columnList = "identifier", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Battery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String identifier;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private SolarSite site;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal capacityKwh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatteryStatus status;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal currentChargePercent;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal currentStoredEnergyKwh;

    @Column(nullable = false)
    private Instant lastUpdatedAt;

    @Version
    private long version;

    @PrePersist
    @PreUpdate
    void normalizeIdentifier() {
        identifier = identifier.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
