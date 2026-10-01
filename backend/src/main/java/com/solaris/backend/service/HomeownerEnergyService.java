package com.solaris.backend.service;

import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.dto.energy.EnergyReadingResponse;
import com.solaris.backend.dto.energy.EnergySummaryResponse;
import com.solaris.backend.entity.EnergyReading;
import com.solaris.backend.exception.BadRequestException;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.EnergyReadingRepository;
import com.solaris.backend.repository.SolarSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class HomeownerEnergyService {
    private static final Duration DEFAULT_RANGE = Duration.ofHours(24);
    private static final Duration MAX_RANGE = Duration.ofDays(366);

    private final EnergyReadingRepository readingRepository;
    private final SolarSiteRepository siteRepository;

    @Transactional(readOnly = true)
    public EnergySummaryResponse summary(Long ownerId, Long siteId, Instant from, Instant to) {
        TimeRange range = range(from, to);
        validateSite(ownerId, siteId);
        var totals = readingRepository.summarize(ownerId, siteId, range.from(), range.to());
        return new EnergySummaryResponse(
                siteId, range.from(), range.to(), totals.getProductionKwh(), totals.getConsumptionKwh(),
                totals.getGridImportKwh(), totals.getGridExportKwh(), totals.getReadingCount());
    }

    @Transactional(readOnly = true)
    public PageResponse<EnergyReadingResponse> readings(
            Long ownerId, Long siteId, Instant from, Instant to, int page, int size) {
        TimeRange range = range(from, to);
        validateSite(ownerId, siteId);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        var readings = siteId == null
                ? readingRepository.findBySiteOwnerIdAndTimestampBetween(ownerId, range.from(), range.to(), pageable)
                : readingRepository.findBySiteOwnerIdAndSiteIdAndTimestampBetween(
                        ownerId, siteId, range.from(), range.to(), pageable);
        return PageResponse.from(readings.map(this::toResponse));
    }

    private void validateSite(Long ownerId, Long siteId) {
        if (siteId != null && siteRepository.findByIdAndOwnerId(siteId, ownerId).isEmpty()) {
            throw new ResourceNotFoundException("Solar site not found");
        }
    }

    private TimeRange range(Instant from, Instant to) {
        Instant resolvedTo = to == null ? Instant.now() : to;
        Instant resolvedFrom = from == null ? resolvedTo.minus(DEFAULT_RANGE) : from;
        if (!resolvedFrom.isBefore(resolvedTo)) {
            throw new BadRequestException("'from' must be before 'to'");
        }
        if (Duration.between(resolvedFrom, resolvedTo).compareTo(MAX_RANGE) > 0) {
            throw new BadRequestException("Energy date range cannot exceed 366 days");
        }
        return new TimeRange(resolvedFrom, resolvedTo);
    }

    private EnergyReadingResponse toResponse(EnergyReading reading) {
        return new EnergyReadingResponse(
                reading.getId(), reading.getSite().getId(), reading.getSite().getName(),
                reading.getDevice().getId(), reading.getDevice().getIdentifier(),
                reading.getProductionKwh(), reading.getConsumptionKwh(), reading.getGridImportKwh(),
                reading.getGridExportKwh(), reading.getTimestamp());
    }

    private record TimeRange(Instant from, Instant to) {
    }
}
