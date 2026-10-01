package com.solaris.backend.service;

import com.solaris.backend.dto.storage.BatteryRequest;
import com.solaris.backend.entity.BatteryStatus;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.exception.BadRequestException;
import com.solaris.backend.repository.AlertRepository;
import com.solaris.backend.repository.BatteryRepository;
import com.solaris.backend.repository.SolarSiteRepository;
import com.solaris.backend.repository.StorageReadingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminBatteryServiceTest {
    @Test
    void rejectsStoredEnergyAboveCapacity() {
        BatteryRepository batteries = mock(BatteryRepository.class);
        SolarSiteRepository sites = mock(SolarSiteRepository.class);
        AdminBatteryService service = new AdminBatteryService(
                batteries, sites, mock(StorageReadingRepository.class), mock(AlertRepository.class));
        User owner = User.builder().role(UserRole.HOMEOWNER).build();
        when(sites.findById(2L)).thenReturn(Optional.of(SolarSite.builder().owner(owner).build()));
        BatteryRequest request = new BatteryRequest();
        request.setIdentifier("BAT-1");
        request.setName("Battery");
        request.setSiteId(2L);
        request.setCapacityKwh(new BigDecimal("10"));
        request.setCurrentStoredEnergyKwh(new BigDecimal("11"));
        request.setCurrentChargePercent(new BigDecimal("50"));
        request.setStatus(BatteryStatus.ONLINE);
        request.setLastUpdatedAt(Instant.now());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot exceed");
    }
}
