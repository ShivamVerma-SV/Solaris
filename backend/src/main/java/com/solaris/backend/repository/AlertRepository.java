package com.solaris.backend.repository;

import com.solaris.backend.entity.Alert;
import com.solaris.backend.entity.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {
    @Override
    @EntityGraph(attributePaths = {"user", "site", "device", "battery"})
    Page<Alert> findAll(@Nullable Specification<Alert> specification, Pageable pageable);

    boolean existsByDeviceId(Long deviceId);

    boolean existsBySiteId(Long siteId);

    boolean existsByBatteryId(Long batteryId);

    @EntityGraph(attributePaths = {"user", "site", "device", "battery"})
    Page<Alert> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "site", "device", "battery"})
    Page<Alert> findByUserIdAndRead(Long userId, boolean read, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "site", "device", "battery"})
    Optional<Alert> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTypeAndDeviceIdAndCreatedAtAfter(
            Long userId, AlertType type, Long deviceId, Instant after);

    boolean existsByUserIdAndTypeAndBatteryIdAndCreatedAtAfter(
            Long userId, AlertType type, Long batteryId, Instant after);

    boolean existsByUserIdAndTypeAndSiteIdAndCreatedAtAfter(
            Long userId, AlertType type, Long siteId, Instant after);
}
