package com.solaris.backend.repository;

import com.solaris.backend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {
    @Override
    @EntityGraph(attributePaths = {"site", "site.owner"})
    Page<Device> findAll(@Nullable Specification<Device> specification, Pageable pageable);

    boolean existsByIdentifier(String identifier);

    boolean existsByIdentifierAndIdNot(String identifier, Long id);

    boolean existsBySiteId(Long siteId);

    @EntityGraph(attributePaths = {"site", "site.owner"})
    List<Device> findByStatusInAndLastSeenAtBefore(
            Collection<com.solaris.backend.entity.DeviceStatus> statuses, Instant cutoff);
}
