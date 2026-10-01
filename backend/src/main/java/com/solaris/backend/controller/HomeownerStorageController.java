package com.solaris.backend.controller;

import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.dto.storage.BatteryResponse;
import com.solaris.backend.dto.storage.StorageReadingResponse;
import com.solaris.backend.security.AuthenticatedUser;
import com.solaris.backend.service.HomeownerStorageService;
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
import java.util.List;

@RestController
@RequestMapping("/api/homeowner/storage")
@RequiredArgsConstructor
@Validated
public class HomeownerStorageController {
    private final HomeownerStorageService service;

    @GetMapping("/status")
    public List<BatteryResponse> status(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.status(user.id());
    }

    @GetMapping("/history")
    public PageResponse<StorageReadingResponse> history(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @Min(1) Long batteryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return service.history(user.id(), batteryId, from, to, page, size);
    }
}
