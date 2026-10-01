package com.solaris.backend.repository;

import com.solaris.backend.entity.Battery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.Collection;

public interface BatteryRepository extends JpaRepository<Battery, Long> {
    boolean existsBySiteId(Long siteId);

    boolean existsByIdentifier(String identifier);

    boolean existsByIdentifierAndIdNot(String identifier, Long id);

    List<Battery> findBySiteOwnerIdOrderByName(Long ownerId);

    Optional<Battery> findByIdAndSiteOwnerId(Long id, Long ownerId);

    List<Battery> findByStatusInAndCurrentChargePercentLessThan(
            Collection<com.solaris.backend.entity.BatteryStatus> statuses, BigDecimal threshold);
}
