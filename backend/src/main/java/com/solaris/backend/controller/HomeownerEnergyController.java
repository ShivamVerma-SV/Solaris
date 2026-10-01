package com.solaris.backend.controller;

import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.dto.energy.EnergyReadingResponse;
import com.solaris.backend.dto.energy.EnergySummaryResponse;
import com.solaris.backend.security.AuthenticatedUser;
import com.solaris.backend.service.HomeownerEnergyService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/homeowner/energy")
@RequiredArgsConstructor
@Validated
public class HomeownerEnergyController {
    private final HomeownerEnergyService service;

    @GetMapping("/summary")
    public EnergySummaryResponse summary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @Min(1) Long siteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.summary(user.id(), siteId, from, to);
    }

    @GetMapping("/readings")
    public PageResponse<EnergyReadingResponse> readings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @Min(1) Long siteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return service.readings(user.id(), siteId, from, to, page, size);
    }
}
