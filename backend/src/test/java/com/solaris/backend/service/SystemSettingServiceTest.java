package com.solaris.backend.service;

import com.solaris.backend.dto.setting.SystemSettingRequest;
import com.solaris.backend.entity.SystemSetting;
import com.solaris.backend.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemSettingServiceTest {
    @Test
    void initializesDefaultsWhenSettingsDoNotExist() {
        SystemSettingRepository repository = mock(SystemSettingRepository.class);
        when(repository.findById(SystemSetting.SINGLETON_ID)).thenReturn(Optional.empty());
        when(repository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = new SystemSettingService(repository).get();

        assertThat(response.lowBatteryThreshold()).isEqualByComparingTo("20.00");
        assertThat(response.deviceOfflineThresholdMinutes()).isEqualTo(30);
        assertThat(response.batteryAlertsEnabled()).isTrue();
        verify(repository).save(any(SystemSetting.class));
    }

    @Test
    void updatesRulesWithoutCreatingAlertRecords() {
        SystemSettingRepository repository = mock(SystemSettingRepository.class);
        SystemSetting existing = SystemSetting.builder().id(1L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        SystemSettingRequest request = new SystemSettingRequest();
        request.setLowBatteryThreshold(new BigDecimal("15"));
        request.setHighConsumptionThresholdKwh(new BigDecimal("12"));
        request.setLowProductionThresholdKwh(new BigDecimal("2"));
        request.setBatteryAlertsEnabled(true);
        request.setDeviceOfflineThresholdMinutes(45);

        var response = new SystemSettingService(repository).update(request);

        assertThat(response.lowBatteryThreshold()).isEqualByComparingTo("15");
        assertThat(response.deviceOfflineThresholdMinutes()).isEqualTo(45);
    }
}
