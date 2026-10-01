package com.solaris.backend.service;

import com.solaris.backend.exception.BadRequestException;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.EnergyReadingRepository;
import com.solaris.backend.repository.SolarSiteRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HomeownerEnergyServiceTest {
    @Test
    void rejectsAnotherHomeownersSiteBeforeQueryingTelemetry() {
        EnergyReadingRepository readings = mock(EnergyReadingRepository.class);
        SolarSiteRepository sites = mock(SolarSiteRepository.class);
        when(sites.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());
        HomeownerEnergyService service = new HomeownerEnergyService(readings, sites);

        assertThatThrownBy(() -> service.summary(
                1L, 10L, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Solar site not found");
        verifyNoInteractions(readings);
    }

    @Test
    void rejectsInvertedDateRange() {
        HomeownerEnergyService service = new HomeownerEnergyService(
                mock(EnergyReadingRepository.class), mock(SolarSiteRepository.class));

        assertThatThrownBy(() -> service.readings(
                1L, null, Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"), 0, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("before");
    }
}
