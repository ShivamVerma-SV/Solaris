package com.solaris.backend.service;

import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.dto.storage.BatteryRequest;
import com.solaris.backend.dto.storage.BatteryResponse;
import com.solaris.backend.entity.Battery;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.exception.BadRequestException;
import com.solaris.backend.exception.ConflictException;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.AlertRepository;
import com.solaris.backend.repository.BatteryRepository;
import com.solaris.backend.repository.SolarSiteRepository;
import com.solaris.backend.repository.StorageReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminBatteryService {
    private final BatteryRepository batteryRepository;
    private final SolarSiteRepository siteRepository;
    private final StorageReadingRepository storageReadingRepository;
    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public PageResponse<BatteryResponse> list(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("identifier").ascending());
        return PageResponse.from(batteryRepository.findAll(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public BatteryResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public BatteryResponse create(BatteryRequest request) {
        String identifier = normalizeIdentifier(request.getIdentifier());
        if (batteryRepository.existsByIdentifier(identifier)) {
            throw new ConflictException("Battery identifier already exists");
        }
        Battery battery = new Battery();
        apply(battery, request, identifier);
        return toResponse(batteryRepository.save(battery));
    }

    @Transactional
    public BatteryResponse update(Long id, BatteryRequest request) {
        Battery battery = find(id);
        String identifier = normalizeIdentifier(request.getIdentifier());
        if (batteryRepository.existsByIdentifierAndIdNot(identifier, id)) {
            throw new ConflictException("Battery identifier already exists");
        }
        apply(battery, request, identifier);
        return toResponse(batteryRepository.save(battery));
    }

    @Transactional
    public void delete(Long id) {
        Battery battery = find(id);
        if (storageReadingRepository.existsByBatteryId(id) || alertRepository.existsByBatteryId(id)) {
            throw new ConflictException("Battery cannot be deleted while storage history or alerts depend on it");
        }
        batteryRepository.delete(battery);
    }

    private void apply(Battery battery, BatteryRequest request, String identifier) {
        SolarSite site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Solar site not found"));
        if (site.getOwner().getRole() != UserRole.HOMEOWNER) {
            throw new BadRequestException("Batteries can only be assigned to homeowner sites");
        }
        if (request.getCurrentStoredEnergyKwh().compareTo(request.getCapacityKwh()) > 0) {
            throw new BadRequestException("Stored energy cannot exceed battery capacity");
        }
        battery.setIdentifier(identifier);
        battery.setName(request.getName().trim());
        battery.setSite(site);
        battery.setCapacityKwh(request.getCapacityKwh());
        battery.setStatus(request.getStatus());
        battery.setCurrentChargePercent(request.getCurrentChargePercent());
        battery.setCurrentStoredEnergyKwh(request.getCurrentStoredEnergyKwh());
        battery.setLastUpdatedAt(request.getLastUpdatedAt());
    }

    private Battery find(Long id) {
        return batteryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Battery not found"));
    }

    BatteryResponse toResponse(Battery battery) {
        return new BatteryResponse(
                battery.getId(), battery.getIdentifier(), battery.getName(), battery.getSite().getId(),
                battery.getSite().getName(), battery.getSite().getOwner().getId(), battery.getCapacityKwh(),
                battery.getStatus(), battery.getCurrentChargePercent(), battery.getCurrentStoredEnergyKwh(),
                battery.getLastUpdatedAt());
    }

    private String normalizeIdentifier(String identifier) {
        return identifier.trim().toUpperCase(Locale.ROOT);
    }
}
