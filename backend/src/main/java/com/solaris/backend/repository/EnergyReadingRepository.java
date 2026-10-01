package com.solaris.backend.repository;

import com.solaris.backend.entity.EnergyReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface EnergyReadingRepository extends JpaRepository<EnergyReading, Long> {
    boolean existsByDeviceId(Long deviceId);

    boolean existsBySiteId(Long siteId);

    Page<EnergyReading> findBySiteOwnerIdAndTimestampBetween(
            Long ownerId, Instant from, Instant to, Pageable pageable);

    Page<EnergyReading> findBySiteOwnerIdAndSiteIdAndTimestampBetween(
            Long ownerId, Long siteId, Instant from, Instant to, Pageable pageable);
}
