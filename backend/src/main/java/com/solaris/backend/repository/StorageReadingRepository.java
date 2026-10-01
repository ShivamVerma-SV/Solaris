package com.solaris.backend.repository;

import com.solaris.backend.entity.StorageReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface StorageReadingRepository extends JpaRepository<StorageReading, Long> {
    boolean existsByBatteryId(Long batteryId);

    boolean existsBySiteId(Long siteId);

    Page<StorageReading> findBySiteOwnerIdAndTimestampBetween(
            Long ownerId, Instant from, Instant to, Pageable pageable);

    Page<StorageReading> findBySiteOwnerIdAndBatteryIdAndTimestampBetween(
            Long ownerId, Long batteryId, Instant from, Instant to, Pageable pageable);
}
