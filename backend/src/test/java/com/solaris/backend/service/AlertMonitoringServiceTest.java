package com.solaris.backend.service;

import com.solaris.backend.entity.Battery;
import com.solaris.backend.entity.BatteryStatus;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.SystemSetting;
import com.solaris.backend.entity.User;
import com.solaris.backend.repository.AlertRepository;
import com.solaris.backend.repository.BatteryRepository;
import com.solaris.backend.repository.DeviceRepository;
import com.solaris.backend.repository.EnergyReadingRepository;
import com.solaris.backend.repository.SolarSiteRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertMonitoringServiceTest {
    @Test
    void cooldownPreventsDuplicateLowBatteryAlert() {
        SystemSettingService settings = mock(SystemSettingService.class);
        BatteryRepository batteries = mock(BatteryRepository.class);
        AlertRepository alerts = mock(AlertRepository.class);
        User owner = User.builder().id(1L).build();
        SolarSite site = SolarSite.builder().id(2L).owner(owner).build();
        Battery battery = Battery.builder().id(3L).identifier("BAT-1").site(site).build();
        SystemSetting setting = SystemSetting.builder()
                .lowBatteryThreshold(new BigDecimal("20"))
                .batteryAlertsEnabled(true)
                .deviceOfflineThresholdMinutes(30)
                .build();
        when(settings.getEntityForUpdate()).thenReturn(setting);
        when(batteries.findByStatusInAndCurrentChargePercentLessThan(
                eq(List.of(BatteryStatus.ONLINE)), eq(new BigDecimal("20"))))
                .thenReturn(List.of(battery));
        when(alerts.existsByUserIdAndTypeAndBatteryIdAndCreatedAtAfter(
                eq(1L), any(), eq(3L), any())).thenReturn(true);

        new AlertMonitoringService(
                settings, mock(DeviceRepository.class), batteries, mock(SolarSiteRepository.class),
                mock(EnergyReadingRepository.class), alerts).evaluate();

        verify(alerts, never()).save(any());
    }
}
