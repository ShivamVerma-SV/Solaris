package com.solaris.backend.repository;

import com.solaris.backend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {
    boolean existsByIdentifier(String identifier);

    boolean existsByIdentifierAndIdNot(String identifier, Long id);

    boolean existsBySiteId(Long siteId);

    List<Device> findByStatusInAndLastSeenAtBefore(
            Collection<com.solaris.backend.entity.DeviceStatus> statuses, Instant cutoff);
}
