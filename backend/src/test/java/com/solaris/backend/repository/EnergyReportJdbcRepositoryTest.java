package com.solaris.backend.repository;

import com.solaris.backend.entity.Device;
import com.solaris.backend.entity.DeviceStatus;
import com.solaris.backend.entity.DeviceType;
import com.solaris.backend.entity.EnergyReading;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EnergyReportJdbcRepositoryTest {
    @Autowired
    private EnergyReportJdbcRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SolarSiteRepository siteRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private EnergyReadingRepository readingRepository;

    @Test
    void aggregatesDailyReadingsAndEnforcesOwnerInSql() {
        User owner = userRepository.save(user("report-owner@example.com"));
        User otherOwner = userRepository.save(user("other-report-owner@example.com"));
        SolarSite site = siteRepository.save(site(owner, "REPORT-SITE"));
        SolarSite otherSite = siteRepository.save(site(otherOwner, "OTHER-REPORT-SITE"));
        Device device = deviceRepository.save(device(site, "REPORT-INV"));
        Device otherDevice = deviceRepository.save(device(otherSite, "OTHER-REPORT-INV"));
        readingRepository.save(reading(site, device, "2026-01-10T08:00:00Z", "3.000", "2.000"));
        readingRepository.save(reading(site, device, "2026-01-10T09:00:00Z", "4.000", "1.500"));
        readingRepository.save(reading(otherSite, otherDevice, "2026-01-10T08:30:00Z", "99.000", "99.000"));
        readingRepository.flush();

        var report = reportRepository.dailyReport(
                owner.getId(), null, Instant.parse("2026-01-10T00:00:00Z"),
                Instant.parse("2026-01-11T00:00:00Z"));

        assertThat(report).hasSize(1);
        assertThat(report.getFirst().siteId()).isEqualTo(site.getId());
        assertThat(report.getFirst().productionKwh()).isEqualByComparingTo("7.000");
        assertThat(report.getFirst().consumptionKwh()).isEqualByComparingTo("3.500");
        assertThat(report.getFirst().readingCount()).isEqualTo(2);
    }

    private User user(String email) {
        return User.builder()
                .name("Report owner")
                .email(email)
                .password("hash")
                .role(UserRole.HOMEOWNER)
                .enabled(true)
                .build();
    }

    private SolarSite site(User owner, String code) {
        return SolarSite.builder()
                .owner(owner)
                .code(code)
                .name(code)
                .capacityKw(new BigDecimal("5.000"))
                .active(true)
                .build();
    }

    private Device device(SolarSite site, String identifier) {
        return Device.builder()
                .site(site)
                .identifier(identifier)
                .name(identifier)
                .type(DeviceType.SOLAR_INVERTER)
                .status(DeviceStatus.ONLINE)
                .build();
    }

    private EnergyReading reading(
            SolarSite site, Device device, String timestamp, String production, String consumption) {
        return EnergyReading.builder()
                .site(site)
                .device(device)
                .productionKwh(new BigDecimal(production))
                .consumptionKwh(new BigDecimal(consumption))
                .gridImportKwh(BigDecimal.ZERO)
                .gridExportKwh(BigDecimal.ZERO)
                .timestamp(Instant.parse(timestamp))
                .build();
    }
}
