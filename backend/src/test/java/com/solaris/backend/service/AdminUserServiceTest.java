package com.solaris.backend.service;

import com.solaris.backend.dto.user.CreateUserRequest;
import com.solaris.backend.dto.user.UpdateUserRequest;
import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.exception.ConflictException;
import com.solaris.backend.exception.ForbiddenException;
import com.solaris.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {
    private UserRepository userRepository;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new AdminUserService(userRepository, new BCryptPasswordEncoder(4));
    }

    @Test
    void createsAdminWithHashedPasswordAndNormalizedEmail() {
        CreateUserRequest request = new CreateUserRequest();
        request.setName(" System Admin ");
        request.setEmail("ADMIN@Example.com");
        request.setPassword("strong-password");
        request.setRole(UserRole.ADMIN);
        request.setEnabled(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        service.createUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("System Admin");
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(captor.getValue().getPassword()).startsWith("$2");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void rejectsDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("duplicate@example.com");
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    void preventsDeletingOwnAccount() {
        assertThatThrownBy(() -> service.deleteUser(8L, 8L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own account");
    }

    @Test
    void preventsDeletingFinalEnabledAdministrator() {
        User admin = user(1L, UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findAllByRoleForUpdate(UserRole.ADMIN)).thenReturn(List.of(admin));

        assertThatThrownBy(() -> service.deleteUser(1L, 2L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("final enabled administrator");
        verify(userRepository, never()).delete(any());
    }

    @Test
    void preventsDemotingFinalEnabledAdministrator() {
        User admin = user(1L, UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findAllByRoleForUpdate(UserRole.ADMIN)).thenReturn(List.of(admin));
        UpdateUserRequest request = updateRequest(UserRole.HOMEOWNER, true);

        assertThatThrownBy(() -> service.updateUser(1L, request))
                .isInstanceOf(ConflictException.class);
    }

    private UpdateUserRequest updateRequest(UserRole role, boolean enabled) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Admin");
        request.setEmail("admin@example.com");
        request.setRole(role);
        request.setEnabled(enabled);
        return request;
    }

    private User user(Long id, UserRole role, boolean enabled) {
        return User.builder()
                .id(id)
                .name("Admin")
                .email("admin@example.com")
                .password("hash")
                .role(role)
                .enabled(enabled)
                .build();
    }
}
