package com.solaris.backend.repository;

import com.solaris.backend.entity.Alert;
import com.solaris.backend.entity.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {
    boolean existsByDeviceId(Long deviceId);

    boolean existsBySiteId(Long siteId);

    boolean existsByBatteryId(Long batteryId);

    Page<Alert> findByUserId(Long userId, Pageable pageable);

    Page<Alert> findByUserIdAndRead(Long userId, boolean read, Pageable pageable);

    Optional<Alert> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTypeAndDeviceIdAndCreatedAtAfter(
            Long userId, AlertType type, Long deviceId, Instant after);

    boolean existsByUserIdAndTypeAndBatteryIdAndCreatedAtAfter(
            Long userId, AlertType type, Long batteryId, Instant after);

    boolean existsByUserIdAndTypeAndSiteIdAndCreatedAtAfter(
            Long userId, AlertType type, Long siteId, Instant after);
}
