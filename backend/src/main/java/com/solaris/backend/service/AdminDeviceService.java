package com.solaris.backend.service;

import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.dto.device.DeviceRequest;
import com.solaris.backend.dto.device.DeviceResponse;
import com.solaris.backend.entity.Device;
import com.solaris.backend.entity.DeviceStatus;
import com.solaris.backend.entity.DeviceType;
import com.solaris.backend.entity.SolarSite;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.exception.BadRequestException;
import com.solaris.backend.exception.ConflictException;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.AlertRepository;
import com.solaris.backend.repository.DeviceRepository;
import com.solaris.backend.repository.EnergyReadingRepository;
import com.solaris.backend.repository.SolarSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminDeviceService {
    private final DeviceRepository deviceRepository;
    private final SolarSiteRepository siteRepository;
    private final EnergyReadingRepository energyReadingRepository;
    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public PageResponse<DeviceResponse> list(int page, int size, DeviceStatus status, DeviceType type, Long siteId) {
        Specification<Device> specification = Specification.allOf();
        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (type != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("type"), type));
        }
        if (siteId != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("site").get("id"), siteId));
        }
        var pageable = PageRequest.of(page, size, Sort.by("identifier").ascending());
        return PageResponse.from(deviceRepository.findAll(specification, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public DeviceResponse get(Long id) {
        return toResponse(findDevice(id));
    }

    @Transactional
    public DeviceResponse create(DeviceRequest request) {
        String identifier = normalizeIdentifier(request.getIdentifier());
        if (deviceRepository.existsByIdentifier(identifier)) {
            throw new ConflictException("Device identifier already exists");
        }
        Device device = new Device();
        apply(device, request, identifier);
        return toResponse(deviceRepository.save(device));
    }

    @Transactional
    public DeviceResponse update(Long id, DeviceRequest request) {
        Device device = findDevice(id);
        String identifier = normalizeIdentifier(request.getIdentifier());
        if (deviceRepository.existsByIdentifierAndIdNot(identifier, id)) {
            throw new ConflictException("Device identifier already exists");
        }
        if (!device.getSite().getId().equals(request.getSiteId())
                && (energyReadingRepository.existsByDeviceId(id) || alertRepository.existsByDeviceId(id))) {
            throw new ConflictException("Device cannot move to another site while telemetry or alerts depend on it");
        }
        apply(device, request, identifier);
        return toResponse(deviceRepository.save(device));
    }

    @Transactional
    public void delete(Long id) {
        Device device = findDevice(id);
        if (energyReadingRepository.existsByDeviceId(id) || alertRepository.existsByDeviceId(id)) {
            throw new ConflictException("Device cannot be deleted while telemetry or alerts depend on it");
        }
        deviceRepository.delete(device);
    }

    private void apply(Device device, DeviceRequest request, String identifier) {
        SolarSite site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Solar site not found"));
        if (site.getOwner().getRole() != UserRole.HOMEOWNER) {
            throw new BadRequestException("Devices can only be assigned to homeowner sites");
        }
        device.setIdentifier(identifier);
        device.setName(request.getName().trim());
        device.setType(request.getType());
        device.setStatus(request.getStatus());
        device.setSite(site);
        device.setManufacturer(normalizeNullable(request.getManufacturer()));
        device.setModel(normalizeNullable(request.getModel()));
        device.setFirmwareVersion(normalizeNullable(request.getFirmwareVersion()));
        device.setLastSeenAt(request.getLastSeenAt());
    }

    private Device findDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    }

    private DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getId(), device.getIdentifier(), device.getName(), device.getType(), device.getStatus(),
                device.getSite().getId(), device.getSite().getName(), device.getSite().getOwner().getId(),
                device.getManufacturer(), device.getModel(), device.getFirmwareVersion(), device.getLastSeenAt(),
                device.getCreatedAt(), device.getUpdatedAt());
    }

    private String normalizeIdentifier(String identifier) {
        return identifier.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
