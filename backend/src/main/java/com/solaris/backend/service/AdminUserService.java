package com.solaris.backend.service;

import com.solaris.backend.dto.user.AdminUserResponse;
import com.solaris.backend.dto.user.CreateUserRequest;
import com.solaris.backend.dto.user.UpdateUserRequest;
import com.solaris.backend.dto.PageResponse;
import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.exception.ConflictException;
import com.solaris.backend.exception.ForbiddenException;
import com.solaris.backend.exception.ResourceNotFoundException;
import com.solaris.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getAllUsers(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(userRepository.findAll(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse createUser(CreateUserRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(request.isEnabled())
                .phone(normalizeNullable(request.getPhone()))
                .build();
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateUser(
            Long id,
            UpdateUserRequest request
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("Email is already registered");
        }
        if (user.getRole() == UserRole.ADMIN
                && user.isEnabled()
                && (request.getRole() != UserRole.ADMIN || !request.isEnabled())) {
            ensureAnotherEnabledAdmin(id);
        }

        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setRole(request.getRole());
        user.setEnabled(request.isEnabled());
        user.setPhone(normalizeNullable(request.getPhone()));
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id, Long currentAdminId) {
        if (id.equals(currentAdminId)) {
            throw new ForbiddenException("Administrators cannot delete their own account");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == UserRole.ADMIN && user.isEnabled()) {
            ensureAnotherEnabledAdmin(id);
        }
        userRepository.delete(user);
    }

    private void ensureAnotherEnabledAdmin(Long excludedId) {
        long otherEnabledAdmins = userRepository.findAllByRoleForUpdate(UserRole.ADMIN).stream()
                .filter(User::isEnabled)
                .filter(admin -> !admin.getId().equals(excludedId))
                .count();
        if (otherEnabledAdmins == 0) {
            throw new ConflictException("The final enabled administrator cannot be removed or disabled");
        }
    }

    private AdminUserResponse toResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
