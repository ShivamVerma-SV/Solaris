package com.solaris.backend.controller;

import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.dto.alert.AlertResponse;
import com.solaris.backend.entity.AlertSeverity;
import com.solaris.backend.entity.AlertType;
import com.solaris.backend.service.AlertService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/alerts")
@RequiredArgsConstructor
@Validated
public class AdminAlertController {
    private final AlertService service;

    @GetMapping
    public PageResponse<AlertResponse> list(
            @RequestParam(required = false) AlertType type,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) @Min(1) Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.adminAlerts(type, severity, read, userId, page, size);
    }
}
