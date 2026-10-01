package com.solaris.backend.service;

import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.dto.alert.AlertResponse;
import com.solaris.backend.entity.Alert;
import com.solaris.backend.entity.AlertSeverity;
import com.solaris.backend.entity.AlertType;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final AlertRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> homeownerAlerts(Long userId, Boolean read, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var alerts = read == null
                ? repository.findByUserId(userId, pageable)
                : repository.findByUserIdAndRead(userId, read, pageable);
        return PageResponse.from(alerts.map(this::toResponse));
    }

    @Transactional
    public AlertResponse markRead(Long userId, Long alertId) {
        Alert alert = repository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        if (!alert.isRead()) {
            alert.setRead(true);
            alert = repository.save(alert);
        }
        return toResponse(alert);
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> adminAlerts(
            AlertType type, AlertSeverity severity, Boolean read, Long userId, int page, int size) {
        Specification<Alert> specification = Specification.allOf();
        if (type != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("type"), type));
        }
        if (severity != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("severity"), severity));
        }
        if (read != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("read"), read));
        }
        if (userId != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("user").get("id"), userId));
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(repository.findAll(specification, pageable).map(this::toResponse));
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(), alert.getType(), alert.getSeverity(), alert.getMessage(), alert.isRead(),
                alert.getCreatedAt(), alert.getUser().getId(),
                alert.getSite() == null ? null : alert.getSite().getId(),
                alert.getDevice() == null ? null : alert.getDevice().getId(),
                alert.getBattery() == null ? null : alert.getBattery().getId());
    }
}
