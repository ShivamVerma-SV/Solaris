package com.solaris.backend.repository;

import com.solaris.backend.entity.Battery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.Collection;

public interface BatteryRepository extends JpaRepository<Battery, Long> {
    @EntityGraph(attributePaths = {"site", "site.owner"})
    Page<Battery> findAll(Pageable pageable);

    boolean existsBySiteId(Long siteId);

    boolean existsByIdentifier(String identifier);

    boolean existsByIdentifierAndIdNot(String identifier, Long id);

    @EntityGraph(attributePaths = {"site", "site.owner"})
    List<Battery> findBySiteOwnerIdOrderByName(Long ownerId);

    @EntityGraph(attributePaths = {"site", "site.owner"})
    Optional<Battery> findByIdAndSiteOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = {"site", "site.owner"})
    List<Battery> findByStatusInAndCurrentChargePercentLessThan(
            Collection<com.solaris.backend.entity.BatteryStatus> statuses, BigDecimal threshold);
}
