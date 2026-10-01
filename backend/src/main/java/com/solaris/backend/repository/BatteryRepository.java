package com.solaris.backend.repository;

import com.solaris.backend.entity.Battery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatteryRepository extends JpaRepository<Battery, Long> {
    boolean existsBySiteId(Long siteId);

    List<Battery> findBySiteOwnerIdOrderByName(Long ownerId);
}
