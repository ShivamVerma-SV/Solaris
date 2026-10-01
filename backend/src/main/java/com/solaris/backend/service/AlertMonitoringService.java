package com.solaris.backend.service;

import com.solaris.backend.entity.Alert;
import com.solaris.backend.entity.AlertSeverity;
import com.solaris.backend.entity.AlertType;
import com.solaris.backend.entity.Battery;
import com.solaris.backend.entity.BatteryStatus;
import com.solaris.backend.entity.Device;
import com.solaris.backend.entity.DeviceStatus;
import com.solaris.backend.entity.EnergyReading;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.SystemSetting;
import com.solaris.backend.repository.AlertRepository;
import com.solaris.backend.repository.BatteryRepository;
import com.solaris.backend.repository.DeviceRepository;
import com.solaris.backend.repository.EnergyReadingRepository;
import com.solaris.backend.repository.SolarSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertMonitoringService {
    private static final Duration ALERT_COOLDOWN = Duration.ofHours(1);

    private final SystemSettingService settingService;
    private final DeviceRepository deviceRepository;
    private final BatteryRepository batteryRepository;
    private final SolarSiteRepository siteRepository;
    private final EnergyReadingRepository energyReadingRepository;
    private final AlertRepository alertRepository;

    @Transactional
    public void evaluate() {
        // The settings row lock serializes monitoring across application instances.
        SystemSetting settings = settingService.getEntityForUpdate();
        Instant now = Instant.now();
        Instant duplicateCutoff = now.minus(ALERT_COOLDOWN);

        if (settings.isDeviceOfflineAlertEnabled()) {
            evaluateOfflineDevices(settings, now, duplicateCutoff);
        }
        if (settings.isBatteryAlertsEnabled()) {
            evaluateBatteries(settings, duplicateCutoff);
        }
        if (settings.isConsumptionAlertsEnabled() || settings.isProductionAlertsEnabled()) {
            evaluateEnergy(settings, now, duplicateCutoff);
        }
    }

    private void evaluateOfflineDevices(SystemSetting settings, Instant now, Instant duplicateCutoff) {
        Instant offlineCutoff = now.minus(Duration.ofMinutes(settings.getDeviceOfflineThresholdMinutes()));
        List<Device> devices = deviceRepository.findByStatusInAndLastSeenAtBefore(
                List.of(DeviceStatus.ONLINE, DeviceStatus.OFFLINE), offlineCutoff);
        for (Device device : devices) {
            Long ownerId = device.getSite().getOwner().getId();
            if (!alertRepository.existsByUserIdAndTypeAndDeviceIdAndCreatedAtAfter(
                    ownerId, AlertType.DEVICE_OFFLINE, device.getId(), duplicateCutoff)) {
                alertRepository.save(Alert.builder()
                        .user(device.getSite().getOwner())
                        .site(device.getSite())
                        .device(device)
                        .type(AlertType.DEVICE_OFFLINE)
                        .severity(AlertSeverity.CRITICAL)
                        .message("Device " + device.getIdentifier()
                                + " has not reported within the configured threshold")
                        .build());
            }
        }
    }

    private void evaluateBatteries(SystemSetting settings, Instant duplicateCutoff) {
        List<Battery> batteries = batteryRepository.findByStatusInAndCurrentChargePercentLessThan(
                List.of(BatteryStatus.ONLINE), settings.getLowBatteryThreshold());
        for (Battery battery : batteries) {
            Long ownerId = battery.getSite().getOwner().getId();
            if (!alertRepository.existsByUserIdAndTypeAndBatteryIdAndCreatedAtAfter(
                    ownerId, AlertType.LOW_BATTERY, battery.getId(), duplicateCutoff)) {
                alertRepository.save(Alert.builder()
                        .user(battery.getSite().getOwner())
                        .site(battery.getSite())
                        .battery(battery)
                        .type(AlertType.LOW_BATTERY)
                        .severity(AlertSeverity.WARNING)
                        .message("Battery " + battery.getIdentifier()
                                + " is below the configured charge threshold")
                        .build());
            }
        }
    }

    private void evaluateEnergy(SystemSetting settings, Instant now, Instant duplicateCutoff) {
        Instant freshnessCutoff = now.minus(ALERT_COOLDOWN);
        for (SolarSite site : siteRepository.findByActiveTrue()) {
            EnergyReading reading = energyReadingRepository.findTopBySiteIdOrderByTimestampDesc(site.getId())
                    .filter(value -> value.getTimestamp().isAfter(freshnessCutoff))
                    .orElse(null);
            if (reading == null) {
                continue;
            }
            Long ownerId = site.getOwner().getId();
            if (settings.isConsumptionAlertsEnabled()
                    && reading.getConsumptionKwh().compareTo(settings.getHighConsumptionThresholdKwh()) > 0
                    && !alertRepository.existsByUserIdAndTypeAndSiteIdAndCreatedAtAfter(
                            ownerId, AlertType.HIGH_CONSUMPTION, site.getId(), duplicateCutoff)) {
                alertRepository.save(Alert.builder()
                        .user(site.getOwner()).site(site)
                        .type(AlertType.HIGH_CONSUMPTION).severity(AlertSeverity.WARNING)
                        .message("Energy consumption at " + site.getName()
                                + " exceeds the configured threshold")
                        .build());
            }
            if (settings.isProductionAlertsEnabled()
                    && reading.getProductionKwh().compareTo(settings.getLowProductionThresholdKwh()) < 0
                    && !alertRepository.existsByUserIdAndTypeAndSiteIdAndCreatedAtAfter(
                            ownerId, AlertType.LOW_PRODUCTION, site.getId(), duplicateCutoff)) {
                alertRepository.save(Alert.builder()
                        .user(site.getOwner()).site(site)
                        .type(AlertType.LOW_PRODUCTION).severity(AlertSeverity.WARNING)
                        .message("Energy production at " + site.getName()
                                + " is below the configured threshold")
                        .build());
            }
        }
    }
}
