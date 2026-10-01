package com.solaris.backend.service;

import com.solaris.backend.dto.device.DeviceRequest;
import com.solaris.backend.entity.Device;
import com.solaris.backend.entity.DeviceStatus;
import com.solaris.backend.entity.DeviceType;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.exception.BadRequestException;
import com.solaris.backend.exception.ConflictException;
import com.solaris.backend.repository.AlertRepository;
import com.solaris.backend.repository.DeviceRepository;
import com.solaris.backend.repository.EnergyReadingRepository;
import com.solaris.backend.repository.SolarSiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDeviceServiceTest {
    private DeviceRepository deviceRepository;
    private SolarSiteRepository siteRepository;
    private EnergyReadingRepository energyReadingRepository;
    private AlertRepository alertRepository;
    private AdminDeviceService service;

    @BeforeEach
    void setUp() {
        deviceRepository = mock(DeviceRepository.class);
        siteRepository = mock(SolarSiteRepository.class);
        energyReadingRepository = mock(EnergyReadingRepository.class);
        alertRepository = mock(AlertRepository.class);
        service = new AdminDeviceService(
                deviceRepository, siteRepository, energyReadingRepository, alertRepository);
    }

    @Test
    void createsDeviceForHomeownerSiteAndNormalizesIdentifier() {
        DeviceRequest request = request();
        when(siteRepository.findById(9L)).thenReturn(Optional.of(site(UserRole.HOMEOWNER)));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> {
            Device device = invocation.getArgument(0);
            device.setId(4L);
            return device;
        });

        var response = service.create(request);

        assertThat(response.identifier()).isEqualTo("INV-001");
        assertThat(response.ownerId()).isEqualTo(2L);
        assertThat(response.siteId()).isEqualTo(9L);
    }

    @Test
    void rejectsDuplicateDeviceIdentifier() {
        DeviceRequest request = request();
        when(deviceRepository.existsByIdentifier("INV-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void rejectsAssignmentToAdminOwnedSite() {
        when(siteRepository.findById(9L)).thenReturn(Optional.of(site(UserRole.ADMIN)));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("homeowner sites");
    }

    @Test
    void blocksDeleteWhenHistoricalTelemetryExists() {
        Device device = Device.builder().id(4L).build();
        when(deviceRepository.findById(4L)).thenReturn(Optional.of(device));
        when(energyReadingRepository.existsByDeviceId(4L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(4L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("telemetry or alerts");
        verify(deviceRepository, never()).delete(any(Device.class));
    }

    private DeviceRequest request() {
        DeviceRequest request = new DeviceRequest();
        request.setIdentifier(" inv-001 ");
        request.setName("Main inverter");
        request.setType(DeviceType.SOLAR_INVERTER);
        request.setStatus(DeviceStatus.ONLINE);
        request.setSiteId(9L);
        return request;
    }

    private SolarSite site(UserRole role) {
        User owner = User.builder().id(2L).name("Owner").role(role).build();
        return SolarSite.builder().id(9L).name("Home").owner(owner).build();
    }
}
