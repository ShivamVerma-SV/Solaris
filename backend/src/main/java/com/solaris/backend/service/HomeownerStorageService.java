package com.solaris.backend.service;

import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.dto.storage.BatteryResponse;
import com.solaris.backend.dto.storage.StorageReadingResponse;
import com.solaris.backend.entity.Battery;
import com.solaris.backend.entity.StorageReading;
import com.solaris.backend.exception.BadRequestException;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.BatteryRepository;
import com.solaris.backend.repository.StorageReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeownerStorageService {
    private static final Duration DEFAULT_RANGE = Duration.ofDays(7);
    private static final Duration MAX_RANGE = Duration.ofDays(366);

    private final BatteryRepository batteryRepository;
    private final StorageReadingRepository readingRepository;

    @Transactional(readOnly = true)
    public List<BatteryResponse> status(Long ownerId) {
        return batteryRepository.findBySiteOwnerIdOrderByName(ownerId).stream()
                .map(this::toBattery)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<StorageReadingResponse> history(
            Long ownerId, Long batteryId, Instant from, Instant to, int page, int size) {
        TimeRange range = range(from, to);
        if (batteryId != null && batteryRepository.findByIdAndSiteOwnerId(batteryId, ownerId).isEmpty()) {
            throw new ResourceNotFoundException("Battery not found");
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        var readings = batteryId == null
                ? readingRepository.findBySiteOwnerIdAndTimestampBetween(ownerId, range.from(), range.to(), pageable)
                : readingRepository.findBySiteOwnerIdAndBatteryIdAndTimestampBetween(
                        ownerId, batteryId, range.from(), range.to(), pageable);
        return PageResponse.from(readings.map(this::toReading));
    }

    private TimeRange range(Instant from, Instant to) {
        Instant resolvedTo = to == null ? Instant.now() : to;
        Instant resolvedFrom = from == null ? resolvedTo.minus(DEFAULT_RANGE) : from;
        if (!resolvedFrom.isBefore(resolvedTo)) {
            throw new BadRequestException("'from' must be before 'to'");
        }
        if (Duration.between(resolvedFrom, resolvedTo).compareTo(MAX_RANGE) > 0) {
            throw new BadRequestException("Storage date range cannot exceed 366 days");
        }
        return new TimeRange(resolvedFrom, resolvedTo);
    }

    private BatteryResponse toBattery(Battery battery) {
        return new BatteryResponse(
                battery.getId(), battery.getIdentifier(), battery.getName(), battery.getSite().getId(),
                battery.getSite().getName(), battery.getSite().getOwner().getId(), battery.getCapacityKwh(),
                battery.getStatus(), battery.getCurrentChargePercent(), battery.getCurrentStoredEnergyKwh(),
                battery.getLastUpdatedAt());
    }

    private StorageReadingResponse toReading(StorageReading reading) {
        return new StorageReadingResponse(
                reading.getId(), reading.getSite().getId(), reading.getSite().getName(),
                reading.getBattery().getId(), reading.getBattery().getIdentifier(), reading.getChargePercent(),
                reading.getStoredEnergyKwh(), reading.getTimestamp());
    }

    private record TimeRange(Instant from, Instant to) {
    }
}
