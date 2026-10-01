package com.solaris.backend.controller;

import com.solaris.backend.dto.report.DailyEnergyReportResponse;
import com.solaris.backend.security.AuthenticatedUser;
import com.solaris.backend.service.EnergyReportService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/homeowner/reports")
@RequiredArgsConstructor
@Validated
public class HomeownerReportController {
    private final EnergyReportService service;

    @GetMapping("/energy")
    public List<DailyEnergyReportResponse> energy(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @Min(1) Long siteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.dailyReport(user.id(), siteId, from, to);
    }
}
