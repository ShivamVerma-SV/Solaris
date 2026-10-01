package com.solaris.backend.controller;

import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.dto.alert.AlertResponse;
import com.solaris.backend.security.AuthenticatedUser;
import com.solaris.backend.service.AlertService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/homeowner/alerts")
@RequiredArgsConstructor
@Validated
public class HomeownerAlertController {
    private final AlertService service;

    @GetMapping
    public PageResponse<AlertResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.homeownerAlerts(user.id(), read, page, size);
    }

    @PutMapping("/{id}/read")
    public AlertResponse markRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return service.markRead(user.id(), id);
    }
}
