package com.solaris.backend.service;

import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.BatteryRepository;
import com.solaris.backend.repository.StorageReadingRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HomeownerStorageServiceTest {
    @Test
    void rejectsHistoryForAnotherHomeownersBattery() {
        BatteryRepository batteries = mock(BatteryRepository.class);
        StorageReadingRepository readings = mock(StorageReadingRepository.class);
        when(batteries.findByIdAndSiteOwnerId(4L, 1L)).thenReturn(Optional.empty());
        HomeownerStorageService service = new HomeownerStorageService(batteries, readings);

        assertThatThrownBy(() -> service.history(
                1L, 4L, Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"), 0, 20))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Battery not found");
        verifyNoInteractions(readings);
    }
}
