package com.solaris.backend.service;

import com.solaris.backend.dto.setting.SystemSettingRequest;
import com.solaris.backend.dto.setting.SystemSettingResponse;
import com.solaris.backend.entity.SystemSetting;
import com.solaris.backend.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SystemSettingService {
    private final SystemSettingRepository repository;

    @Transactional
    public SystemSettingResponse get() {
        return toResponse(getOrCreate());
    }

    @Transactional
    public SystemSettingResponse update(SystemSettingRequest request) {
        SystemSetting setting = getOrCreate();
        setting.setLowBatteryThreshold(request.getLowBatteryThreshold());
        setting.setHighConsumptionThresholdKwh(request.getHighConsumptionThresholdKwh());
        setting.setLowProductionThresholdKwh(request.getLowProductionThresholdKwh());
        setting.setProductionAlertsEnabled(request.isProductionAlertsEnabled());
        setting.setConsumptionAlertsEnabled(request.isConsumptionAlertsEnabled());
        setting.setBatteryAlertsEnabled(request.isBatteryAlertsEnabled());
        setting.setDeviceOfflineAlertEnabled(request.isDeviceOfflineAlertEnabled());
        setting.setDeviceOfflineThresholdMinutes(request.getDeviceOfflineThresholdMinutes());
        setting.setEmailNotificationsEnabled(request.isEmailNotificationsEnabled());
        return toResponse(repository.save(setting));
    }

    @Transactional
    public SystemSetting getEntity() {
        return getOrCreate();
    }

    @Transactional
    public SystemSetting getEntityForUpdate() {
        return repository.findLockedById(SystemSetting.SINGLETON_ID)
                .orElseGet(() -> repository.save(defaults()));
    }

    private SystemSetting getOrCreate() {
        return repository.findById(SystemSetting.SINGLETON_ID)
                .orElseGet(() -> repository.save(defaults()));
    }

    private SystemSetting defaults() {
        return SystemSetting.builder()
                .id(SystemSetting.SINGLETON_ID)
                .lowBatteryThreshold(new BigDecimal("20.00"))
                .highConsumptionThresholdKwh(new BigDecimal("10.000"))
                .lowProductionThresholdKwh(new BigDecimal("1.000"))
                .productionAlertsEnabled(true)
                .consumptionAlertsEnabled(true)
                .batteryAlertsEnabled(true)
                .deviceOfflineAlertEnabled(true)
                .deviceOfflineThresholdMinutes(30)
                .emailNotificationsEnabled(false)
                .build();
    }

    private SystemSettingResponse toResponse(SystemSetting setting) {
        return new SystemSettingResponse(
                setting.getLowBatteryThreshold(), setting.getHighConsumptionThresholdKwh(),
                setting.getLowProductionThresholdKwh(), setting.isProductionAlertsEnabled(),
                setting.isConsumptionAlertsEnabled(), setting.isBatteryAlertsEnabled(),
                setting.isDeviceOfflineAlertEnabled(), setting.getDeviceOfflineThresholdMinutes(),
                setting.isEmailNotificationsEnabled(), setting.getUpdatedAt());
    }
}
