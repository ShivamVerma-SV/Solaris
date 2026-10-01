package com.solaris.backend.repository;

import com.solaris.backend.entity.EnergyReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface EnergyReadingRepository extends JpaRepository<EnergyReading, Long> {
    boolean existsByDeviceId(Long deviceId);

    boolean existsBySiteId(Long siteId);

    Page<EnergyReading> findBySiteOwnerIdAndTimestampBetween(
            Long ownerId, Instant from, Instant to, Pageable pageable);

    Page<EnergyReading> findBySiteOwnerIdAndSiteIdAndTimestampBetween(
            Long ownerId, Long siteId, Instant from, Instant to, Pageable pageable);

    @Query("""
            select coalesce(sum(e.productionKwh), 0),
                   coalesce(sum(e.consumptionKwh), 0),
                   coalesce(sum(e.gridImportKwh), 0),
                   coalesce(sum(e.gridExportKwh), 0),
                   count(e)
            from EnergyReading e
            where e.site.owner.id = :ownerId
              and e.timestamp between :from and :to
              and (:siteId is null or e.site.id = :siteId)
            """)
    Object[] summarize(
            @Param("ownerId") Long ownerId,
            @Param("siteId") Long siteId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
