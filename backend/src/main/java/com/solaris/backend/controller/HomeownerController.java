package com.solaris.backend.controller;

import com.solaris.backend.dto.homeowner.ProfileResponse;
import com.solaris.backend.dto.homeowner.UpdateProfileRequest;
import com.solaris.backend.dto.site.SiteResponse;
import com.solaris.backend.security.AuthenticatedUser;
import com.solaris.backend.service.HomeownerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/homeowner")
@RequiredArgsConstructor
public class HomeownerController {
    private final HomeownerService service;

    @GetMapping("/profile")
    public ProfileResponse profile(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.profile(user.id());
    }

    @PutMapping("/profile")
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return service.updateProfile(user.id(), request);
    }

    @GetMapping("/sites")
    public List<SiteResponse> sites(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.sites(user.id());
    }

    @GetMapping("/sites/{id}")
    public SiteResponse site(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return service.site(user.id(), id);
    }
}
