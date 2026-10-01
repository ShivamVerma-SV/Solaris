package com.solaris.backend.controller;

import com.solaris.backend.dto.user.AdminUserResponse;
import com.solaris.backend.dto.user.CreateUserRequest;
import com.solaris.backend.dto.user.UpdateUserRequest;
import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.security.AuthenticatedUser;
import com.solaris.backend.service.AdminUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<PageResponse<AdminUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(adminUserService.getAllUsers(page, size));
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUserById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                adminUserService.getUserById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(
                adminUserService.updateUser(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentAdmin
    ) {
        adminUserService.deleteUser(id, currentAdmin.id());

        return ResponseEntity.noContent().build();
    }
}
