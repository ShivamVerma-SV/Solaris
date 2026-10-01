package com.solaris.backend.controller;

import com.solaris.backend.dto.setting.SystemSettingRequest;
import com.solaris.backend.dto.setting.SystemSettingResponse;
import com.solaris.backend.service.SystemSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class AdminSystemSettingController {
    private final SystemSettingService service;

    @GetMapping
    public SystemSettingResponse get() {
        return service.get();
    }

    @PutMapping
    public SystemSettingResponse update(@Valid @RequestBody SystemSettingRequest request) {
        return service.update(request);
    }
}
