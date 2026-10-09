package com.solaris.backend.service;

import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.dto.site.SiteRequest;
import com.solaris.backend.dto.site.SiteResponse;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.exception.BadRequestException;
import com.solaris.backend.exception.ConflictException;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.AlertRepository;
import com.solaris.backend.repository.BatteryRepository;
import com.solaris.backend.repository.DeviceRepository;
import com.solaris.backend.repository.EnergyReadingRepository;
import com.solaris.backend.repository.SolarSiteRepository;
import com.solaris.backend.repository.StorageReadingRepository;
import com.solaris.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminSiteService {
    private final SolarSiteRepository siteRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final BatteryRepository batteryRepository;
    private final EnergyReadingRepository energyReadingRepository;
    private final StorageReadingRepository storageReadingRepository;
    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public PageResponse<SiteResponse> list(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return PageResponse.from(siteRepository.findAll(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public SiteResponse get(Long id) {
        return toResponse(findSite(id));
    }

    @Transactional
    public SiteResponse create(SiteRequest request) {
        String code = normalizeCode(request.getCode());
        if (siteRepository.existsByCode(code)) {
            throw new ConflictException("Site code already exists");
        }
        User owner = homeowner(request.getOwnerId());
        SolarSite site = SolarSite.builder()
                .owner(owner)
                .code(code)
                .name(request.getName().trim())
                .address(normalizeNullable(request.getAddress()))
                .capacityKw(request.getCapacityKw())
                .active(request.isActive())
                .build();
        return toResponse(siteRepository.save(site));
    }

    @Transactional
    public SiteResponse update(Long id, SiteRequest request) {
        SolarSite site = findSite(id);
        String code = normalizeCode(request.getCode());
        if (siteRepository.existsByCodeAndIdNot(code, id)) {
            throw new ConflictException("Site code already exists");
        }
        User owner = homeowner(request.getOwnerId());
        if (!site.getOwner().getId().equals(owner.getId()) && hasDependents(id)) {
            // Reassigning a live site would silently move its historical telemetry and alerts to another user.
            throw new ConflictException("Site ownership cannot change while operational or historical data depends on it");
        }
        site.setOwner(owner);
        site.setCode(code);
        site.setName(request.getName().trim());
        site.setAddress(normalizeNullable(request.getAddress()));
        site.setCapacityKw(request.getCapacityKw());
        site.setActive(request.isActive());
        return toResponse(siteRepository.save(site));
    }

    @Transactional
    public void delete(Long id) {
        SolarSite site = findSite(id);
        if (hasDependents(id)) {
            // Keep operational history intact instead of relying on cascading deletes for domain data.
            throw new ConflictException("Site cannot be deleted while devices, batteries, telemetry, or alerts depend on it");
        }
        siteRepository.delete(site);
    }

    private boolean hasDependents(Long siteId) {
        return deviceRepository.existsBySiteId(siteId)
                || batteryRepository.existsBySiteId(siteId)
                || energyReadingRepository.existsBySiteId(siteId)
                || storageReadingRepository.existsBySiteId(siteId)
                || alertRepository.existsBySiteId(siteId);
    }

    private User homeowner(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site owner not found"));
        if (user.getRole() != UserRole.HOMEOWNER) {
            throw new BadRequestException("Solar sites can only be assigned to homeowners");
        }
        return user;
    }

    private SolarSite findSite(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solar site not found"));
    }

    private SiteResponse toResponse(SolarSite site) {
        return new SiteResponse(
                site.getId(), site.getOwner().getId(), site.getOwner().getName(), site.getCode(), site.getName(),
                site.getAddress(), site.getCapacityKw(), site.isActive(), site.getCreatedAt(), site.getUpdatedAt());
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
