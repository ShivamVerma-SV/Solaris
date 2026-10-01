package com.solaris.backend.repository;

import com.solaris.backend.entity.SolarSite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface SolarSiteRepository extends JpaRepository<SolarSite, Long>, JpaSpecificationExecutor<SolarSite> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    @EntityGraph(attributePaths = "owner")
    Page<SolarSite> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<SolarSite> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    List<SolarSite> findByOwnerIdOrderByName(Long ownerId);

    @EntityGraph(attributePaths = "owner")
    Optional<SolarSite> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = "owner")
    List<SolarSite> findByActiveTrue();
}
